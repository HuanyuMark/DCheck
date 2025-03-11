//package org.example.dcheck.spi;
//
//import lombok.Getter;
//import org.example.dcheck.api.DCheckTokenizer;
//
//import java.util.Properties;
//
///**
// * Date 2025/03/11
// *
// * @author 三石而立Sunsy
// */
//public class TokenizerMapProvider {
//    @Getter
//    private final static TokenizerMapProvider instance = new TokenizerMapProvider();
//
//    @Getter(lazy = true)
//    private final Properties tokenizerMap = Providers.loadConfig("tokenizer-map");
//
////    public DCheckTokenizer getDefaultTokenizer() {
////        var tokenizerClassNames = tokenizerMap.entrySet().stream().filter(e -> e.getKey() instanceof String)
////                .map(e -> ((String) e.getValue()))
////                .collect(Collectors.toSet());
////
////    }
//
//    public DCheckTokenizer getTokenizer(String name) {
//        String tokenizerClassname = tokenizerMap.getProperty(name);
//        if (tokenizerClassname == null) {
//            throw new IllegalArgumentException("unmatched tokenizer '': please list these tokenizer in classpath by SPI");
//        }
//        return Providers.createService(tokenizerMap, "tokenizer", tokenizerClassname);
//    }
//}
