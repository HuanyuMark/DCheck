package org.example.dcheck.impl;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.example.dcheck.api.embedding.Embedding;
import org.example.dcheck.api.embedding.EmbeddingFunction;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.clip.ClipByValue;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.shade.guava.primitives.Floats;
import tech.amikos.chromadb.EFException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Slf4j
@SuppressWarnings("unused")
public class DefaultEmbeddingFunction implements EmbeddingFunction {

    private final TextEmbeddingFunction textEmbeddingFunction = new TextEmbeddingFunction();

    @Override
    public List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) {
        //TODO 支持多模态嵌入
        throw new UnsupportedOperationException();
    }

    @Override
    public void init() throws EFException {
        textEmbeddingFunction.init();
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        return textEmbeddingFunction.embedQuery(query);
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        return textEmbeddingFunction.embedDocuments(documents);
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return textEmbeddingFunction.embedDocuments(documents);
    }

    protected static class TextEmbeddingFunction implements EmbeddingFunction {

        public static final String MODEL_NAME = "all-MiniLM-L6-v2";
        public static final Path MODEL_CACHE_DIR = Paths.get("dcheck-env", "cache", "chroma", "onnx_models", MODEL_NAME);
        private static final Path modelPath = MODEL_CACHE_DIR.resolve("onnx");
        private static final Path modelFile = modelPath.resolve("model.onnx");
        private static final String ARCHIVE_FILENAME = "onnx.tar.gz";
        private static final String MODEL_DOWNLOAD_URL = "https://chroma-onnx-models.s3.amazonaws.com/all-MiniLM-L6-v2/onnx.tar.gz";
        private static final String MODEL_SHA256_CHECKSUM = "913d7300ceae3b2dbc2c50d1de4baacab4be7b9380491c27fab7418616a16ec3";
        OrtSession session;
        private HuggingFaceTokenizer tokenizer;
        private OrtEnvironment env;
        private boolean init;

        public TextEmbeddingFunction() {
        }

        public static float[][] normalize(float[][] v) {
            int rows = v.length;
            int cols = v[0].length;
            float[] norm = new float[rows];

            // Step 1: Compute the L2 norm of each row
            for (int i = 0; i < rows; i++) {
                float sum = 0;
                for (int j = 0; j < cols; j++) {
                    sum += v[i][j] * v[i][j];
                }
                norm[i] = (float) Math.sqrt(sum);
            }

            // Step 2: Handle zero norms
            for (int i = 0; i < rows; i++) {
                if (norm[i] == 0) {
                    norm[i] = 1e-12f;
                }
            }

            // Step 3: Normalize each row
            float[][] normalized = new float[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    normalized[i][j] = v[i][j] / norm[i];
                }
            }
            return normalized;
        }

        private static String getSHA256Checksum(String filePath) throws IOException, NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(filePath)) {
                byte[] byteArray = new byte[1024];
                int bytesCount;
                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        private static void extractTarGz(Path tarGzPath, Path extractDir) throws IOException {
            try (InputStream fileIn = Files.newInputStream(tarGzPath);
                 GZIPInputStream gzipIn = new GZIPInputStream(fileIn);
                 TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

                TarArchiveEntry entry;
                while ((entry = tarIn.getNextEntry()) != null) {
                    Path entryPath = extractDir.resolve(entry.getName());
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (OutputStream out = Files.newOutputStream(entryPath)) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = tarIn.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void init() throws EFException {
            if (init) return;
            if (!validateModel()) {
                downloadAndSetupModel();
            }

            Map<String, String> tokenizerConfig = Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("padding", "MAX_LENGTH");
                put("maxLength", "512");
            }});

            try {
                tokenizer = HuggingFaceTokenizer.newInstance(modelPath, tokenizerConfig);

                this.env = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions options = new OrtSession.SessionOptions();
                this.session = env.createSession(modelFile.toString(), options);
            } catch (OrtException | IOException e) {
                throw new EFException(e);
            }
            init = true;
        }

        public List<List<Float>> forward(List<String> documents) throws OrtException, EFException {

            ArrayList<AutoCloseable> resources = new ArrayList<>();
            try {
                Encoding[] e = tokenizer.batchEncode(documents, true, false);


                int maxIds = 0;
                int inputIdsLength = 0;
                int attentionMaskLength = 0;
                int tokenIdtypesLength = 0;
                for (Encoding encoding : e) {
                    maxIds = Math.max(maxIds, encoding.getIds().length);
                    inputIdsLength += encoding.getIds().length;
                    attentionMaskLength += encoding.getAttentionMask().length;
                    tokenIdtypesLength += encoding.getTypeIds().length;
                }

                long[] inputIds = new long[inputIdsLength];
                long[] attentionMask = new long[attentionMaskLength];
                long[] tokenIdtypes = new long[tokenIdtypesLength];

                int inputIdsCur = 0;
                int attentionMaskCur = 0;
                int tokenIdtypesCur = 0;
                for (Encoding encoding : e) {
                    System.arraycopy(encoding.getIds(), 0, inputIds, inputIdsCur, encoding.getIds().length);
                    System.arraycopy(encoding.getAttentionMask(), 0, attentionMask, attentionMaskCur, encoding.getAttentionMask().length);
                    System.arraycopy(encoding.getTypeIds(), 0, tokenIdtypes, tokenIdtypesCur, encoding.getTypeIds().length);
                    inputIdsCur += encoding.getIds().length;
                    attentionMaskCur += encoding.getAttentionMask().length;
                    tokenIdtypesCur += encoding.getTypeIds().length;
                }
//
//                ArrayList<Long> inputIds = new ArrayList<>();
//                ArrayList<Long> attentionMask = new ArrayList<>();
//                ArrayList<Long> tokenIdtypes = new ArrayList<>();
//                int maxIds = 0;
//                for (Encoding encoding : e) {
//                    maxIds = Math.max(maxIds, encoding.getIds().length);
//                    List<Long> longs = Arrays.asList(Arrays.stream(encoding.getIds()).boxed().toArray(Long[]::new));
//                    inputIds.addAll(longs);
//                    attentionMask.addAll(Arrays.asList(Arrays.stream(encoding.getAttentionMask()).boxed().toArray(Long[]::new)));
//                    tokenIdtypes.addAll(Arrays.asList(Arrays.stream(encoding.getTypeIds()).boxed().toArray(Long[]::new)));
//                }
                long[] inputShape = new long[]{e.length, maxIds};

                OnnxTensor inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds), inputShape);
                resources.add(inputTensor);
                OnnxTensor attentionTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), inputShape);
                resources.add(attentionTensor);
                OnnxTensor _tokenIdtypes = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenIdtypes), inputShape);
                resources.add(_tokenIdtypes);


                // Inputs for all-MiniLM-L6-v2 model
                Map<String, ? extends OnnxTensorLike> inputs = Collections.unmodifiableMap(new HashMap<String, OnnxTensorLike>() {{
                    put("input_ids", inputTensor);
                    put("attention_mask", attentionTensor);
                    put("token_type_ids", _tokenIdtypes);
                }});

                INDArray lastHiddenState;
                try (OrtSession.Result results = session.run(inputs)) {
                    lastHiddenState = Nd4j.create((float[][][]) results.get(0).getValue());
                    resources.add(lastHiddenState);
                }

                INDArray attMask = Nd4j.create(Arrays.stream(attentionMask).mapToDouble(i -> i).toArray(), inputShape, 'c');
                resources.add(attMask);
                INDArray expandedMask = Nd4j.expandDims(attMask, 2).broadcast(lastHiddenState.shape());
                resources.add(expandedMask);
                INDArray summed = lastHiddenState.mul(expandedMask).sum(1);
                resources.add(summed);
                INDArray[] clippedSumMask = Nd4j.getExecutioner().exec(
                        new ClipByValue(expandedMask.sum(1), 1e-9, Double.MAX_VALUE)
                );
                resources.addAll(Arrays.asList(clippedSumMask));

                INDArray embeddings = summed.div(clippedSumMask[0]);
                resources.add(embeddings);
                float[][] embeddingsArray = normalize(embeddings.toFloatMatrix());
                List<List<Float>> embeddingsList = new ArrayList<>();
                for (float[] embedding : embeddingsArray) {
                    embeddingsList.add(Floats.asList(embedding));
                }
                return embeddingsList;
            } finally {
                resources.forEach(r -> {
                    try {
                        r.close();
                    } catch (Exception ex) {
                        log.warn("error close resource: {}", r.getClass(), ex);
                    }
                });
            }
        }

        private void downloadAndSetupModel() throws EFException {
            try (InputStream in = new URL(MODEL_DOWNLOAD_URL).openStream()) {
                if (!Files.exists(MODEL_CACHE_DIR)) {
                    Files.createDirectories(MODEL_CACHE_DIR);
                }

                Path archivePath = MODEL_CACHE_DIR.resolve(ARCHIVE_FILENAME);
                if (!archivePath.toFile().exists()) {
                    log.warn("Model not found under {}. Downloading...", archivePath);
                    Files.copy(in, archivePath, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!MODEL_SHA256_CHECKSUM.equals(getSHA256Checksum(archivePath.toString()))) {
                    throw new RuntimeException("Checksum does not match. Delete the whole directory " + MODEL_CACHE_DIR + " and try again.");
                }
                extractTarGz(archivePath, MODEL_CACHE_DIR);
                archivePath.toFile().delete();
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new EFException(e);
            }
        }


        /**
         * Check if the model is present at the expected location
         *
         */
        private boolean validateModel() {
            return modelFile.toFile().exists() && modelFile.toFile().isFile();
        }

        @Override
        public Embedding embedQuery(String query) throws EFException {
            try {
                return Embedding.fromList(forward(Collections.singletonList(query)).get(0));
            } catch (OrtException e) {
                throw new EFException(e);
            }
        }

        @Override
        public List<Embedding> embedDocuments(List<String> documents) throws EFException {
            try {
                return forward(documents).stream().map(Embedding::new).collect(Collectors.toList());
            } catch (OrtException e) {
                throw new EFException(e);
            }
        }

        @Override
        public List<Embedding> embedDocuments(String[] documents) throws EFException {
            return embedDocuments(Arrays.asList(documents));
        }

        @Override
        public List<Embedding> embedUnknownTypeDocuments(List<Supplier<InputStream>> documents) {
            throw new UnsupportedOperationException();
        }
    }
}
