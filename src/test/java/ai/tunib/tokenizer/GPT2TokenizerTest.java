package ai.tunib.tokenizer;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GPT2TokenizerTest {
    private final String encodingExample = "Hello my name is Kevin.";
    private final List<Integer> decodingExample = List.of(15496, 616, 1438, 318, 7939, 13);

    @Test
    public void testEncoding() {
        GPT2Tokenizer tokenizer = GPT2Tokenizer.fromPretrained("tokenizers/gpt2");
        List<Integer> result = tokenizer.encode(encodingExample);
        Assertions.assertEquals(decodingExample, result);
    }

    @Test
    public void testDecoding() {
        GPT2Tokenizer tokenizer = GPT2Tokenizer.fromPretrained("tokenizers/gpt2");
        String result = tokenizer.decode(decodingExample);
        Assertions.assertEquals(encodingExample, result);
    }
}
