package org.example.dcheck.impl;

import lombok.Data;
import org.example.dcheck.api.Content;
import org.example.dcheck.api.Document;

/**
 * Date 2025/03/03
 *
 * @author 三石而立Sunsy
 */
@Data
public class UnknownDocument implements Document {
    private final String id;
    private final Content content;
}
