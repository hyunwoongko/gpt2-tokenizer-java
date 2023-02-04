package ai.tunib.tokenizer;

import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.primitives.Chars;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.tomcat.util.json.JSONParser;
import org.apache.tomcat.util.json.ParseException;
import org.springframework.core.io.ClassPathResource;

public class GPT2Tokenizer {
    private List<String> bpe;
    private Map<String, Object> encoder;
    private Map<Object, String> decoder;

    private Map<String, String> cache = Maps.newHashMap();
    private Map<Integer, String> byte2unicode = byteToUnicode();
    private Map<MutablePair<String, String>, Integer> bpeRanks = Maps.newHashMap();
    private Pattern pattern = Pattern.compile("'s|'t|'re|'ve|'m|'ll|'d| ?\\p{L}+| ?\\p{N}+| ?[^\\s\\p{L}\\p{N}]+|\\s+(?!\\S)|\\s+");

    private GPT2Tokenizer(String path) {
        ClassPathResource encoderFile = new ClassPathResource(Paths.get(path, Constants.ENCODER_FILE_NAME).toString());
        ClassPathResource bpeFile = new ClassPathResource(Paths.get(path, Constants.VOCAB_FILE_NAME).toString());
        try {
            this.encoder = new JSONParser(new InputStreamReader(encoderFile.getInputStream(), StandardCharsets.UTF_8)).parseObject();
            this.decoder = encoder.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
            this.bpe = Files.readAllLines(Paths.get(bpeFile.getURI()), StandardCharsets.UTF_8);

            for (int i = 0; i < this.bpe.size(); i++) {
                String[] pairs = bpe.get(i).split(" ");
                this.bpeRanks.put(MutablePair.of(pairs[0], pairs[1]), i);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public static GPT2Tokenizer fromPretrained(String path) {
        return new GPT2Tokenizer(path);
    }

    private HashSet<MutablePair<String, String>> getPairs(List<String> word) {
        HashSet<MutablePair<String, String>> pairs = Sets.newHashSet();
        String prevCharacter = word.get(0);
        for (String character : word.subList(1, word.size())) {
            pairs.add(new MutablePair<>(prevCharacter, character));
            prevCharacter = character;
        }
        return pairs;
    }

    private Map<Integer, String> byteToUnicode() {
        List<Integer> bs = Stream.of(
                                     IntStream.range('!', '~' + 1).boxed(),
                                     IntStream.range('¡', '¬' + 1).boxed(),
                                     IntStream.range('®', 'ÿ' + 1).boxed()
                                 ).reduce(Stream::concat)
                                 .get()
                                 .collect(Collectors.toList());
        List<Integer> cs = new ArrayList<>(List.copyOf(bs));

        int n = 0;
        int max = (int)Math.pow(2, 8);
        for (int b = 0; b < max; b++) {
            if (!bs.contains(b)) {
                bs.add(b);
                cs.add(max + n);
                n += 1;
            }
        }
        List<String> csString = cs.stream()
                                  .map(i -> String.valueOf(Character.toChars(i)))
                                  .collect(Collectors.toList());

        Map<Integer, String> output = Maps.newHashMap();
        for (int i = 0; i < bs.size(); i++) {
            output.put(bs.get(i), csString.get(i));
        }
        return output;
    }

    private String bpe(String token) {
        if (cache.containsKey(token)) {
            return cache.get(token);
        }

        List<String> word = token.chars()
                                 .mapToObj(i -> String.valueOf((char)i))
                                 .collect(Collectors.toList());

        HashSet<MutablePair<String, String>> pairs = getPairs(word);

        while (true) {
            int minScore = Integer.MAX_VALUE;
            MutablePair<String, String> biGram = null;

            for (MutablePair<String, String> pair : pairs) {
                if (bpeRanks.containsKey(pair)) {
                    int score = bpeRanks.get(pair);

                    if (score < minScore) {
                        minScore = score;
                        biGram = pair;
                    }
                }
            }

            if (biGram == null) {
                break;
            }

            String first = biGram.left;
            String second = biGram.right;
            List<String> newWord = new ArrayList<>();
            int i = 0;

            while (i < word.size()) {
                int j = indexWithStartPosition(word, first, i);

                if (j != -1) {
                    newWord.addAll(word.subList(i, j));
                    i = j;
                }
                else {
                    newWord.addAll(word.subList(i, word.size()));
                    break;
                }

                if (word.get(i).equals(first) && i < word.size() - 1 && word.get(i + 1).equals(second)) {
                    newWord.add(first + second);
                    i += 2;
                }
                else {
                    newWord.add(word.get(i));
                    i += 1;
                }
            }

            word = newWord;
            if (word.size() == 1) {
                break;
            }
            else {
                pairs = getPairs(word);
            }
        }

        String output = String.join(" ", word);
        cache.put(token, output);
        return output;
    }

    private <T> int indexWithStartPosition(List<T> list, T find, int startPosition) {
        if (list == null || list.isEmpty()) {
            return -1;
        }
        for (int index = startPosition; index < list.size(); index++) {
            if (list.get(index).equals(find)) {
                return index;
            }
        }
        return -1;
    }

    public List<Integer> encode(String text) {
        Matcher matcher = pattern.matcher(text);
        List<String> unicodes = new ArrayList<>();
        List<Integer> bpeTokens = new ArrayList<>();

        while (matcher.find()) {
            String match = matcher.group();
            StringBuilder unicodeBuilder = new StringBuilder();
            for (byte b : match.getBytes(StandardCharsets.UTF_8)) {
                unicodeBuilder.append(this.byte2unicode.get((int)b));
            }
            unicodes.add(unicodeBuilder.toString());
        }

        for (String token : unicodes) {
            for (String bpeToken : bpe(token).split(" ")) {
                bpeTokens.add(((BigInteger)encoder.get(bpeToken)).intValue());
            }
        }

        return bpeTokens;
    }

    public String decode(List<Integer> tokens) {
        StringBuilder textBuilder = new StringBuilder();
        List<String> byteBufferList = new ArrayList<>();

        for (int token : tokens) {
            textBuilder.append(decoder.get(BigInteger.valueOf(token)));
        }
        String text = textBuilder.toString();

        for (int i = 0; i < text.length(); i++) {
            byteBufferList.add(byte2unicode.get((int)text.charAt(i)));
        }

        byte[] byteBuffer = new byte[byteBufferList.size()];
        for (int i = 0; i < byteBuffer.length; i++) {
            String byteString = byteBufferList.get(i);
            if (byteString == null) {
                byteString = " ";
            }
            byteBuffer[i] = (byte)byteString.charAt(0);
        }

        return Chars.asList(StandardCharsets.UTF_8
                                .decode(ByteBuffer.wrap(byteBuffer))
                                .array())
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining());
    }
}

