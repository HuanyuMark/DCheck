package org.example.dcheck.common.util;

import org.example.dcheck.api.Content;

/**
 * Date: 2025/3/1
 *
 * @author 三石而立Sunsy
 */
@SuppressWarnings("unused")
public class ContentConvert {

    //TODO 引入 commons-io 包
    public static String castToText(Content content) {
        //TODO support other content type
//        try {
//            return content instanceof InMemoryTextContent ? ((InMemoryTextContent) content).getText().toString() :
//                    content instanceof TextContent ? new String(IOUtils.toByteArray(content.getInputStream())) : "";
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        return null;
    }
}
