# GPT2 Tokenizer Java
Java implementation of GPT2 tokenizer

## Requirements
Please install the following dependencies to use the library.

```
implementation 'com.google.api-client:google-api-client:1.32.2'
implementation 'org.apache.commons:commons-lang3:3.12.0'
implementation 'org.springframework.boot:spring-boot-starter-web'

testImplementation 'org.junit.jupiter:junit-jupiter-api:5.3.1'
testRuntimeOnly 'org.junit.jupiter:junit-jupiter-engine:5.3.1'
```

## Add tokenizer files to resources directory
Please add `encoder.json` and `vocab.bpe` files to your project resources directory.
these files can be found [here](https://github.com/hyunwoongko/gpt2-tokenizer-java/tree/master/src/main/resources/tokenizers/gpt2).

## Usage
The following are simple examples of this library.
To check test code for this, refer to [here](https://github.com/hyunwoongko/gpt2-tokenizer-java/blob/master/src/test/java/ai/tunib/tokenizer/GPT2TokenizerTest.java).

### Encoding text to tokens
```java
import ai.tunib.tokenizer.GPT2Tokenizer;
import java.util.List;

GPT2Tokenizer tokenizer = GPT2Tokenizer.fromPretrained("PATH/IN/RESOURCES");
List<Integer> result = tokenizer.encode("Hello my name is Kevin.");
```
```
[15496, 616, 1438, 318, 7939, 13]
```

### Decoding tokens to text
```java
import ai.tunib.tokenizer.GPT2Tokenizer;
import java.util.List;

GPT2Tokenizer tokenizer = GPT2Tokenizer.fromPretrained("PATH/IN/RESOURCES");
List<Integer> result = tokenizer.decode(List.of(15496, 616, 1438, 318, 7939, 13));
```
```
"Hello my name is Kevin."
```

## License

This project is licensed under the terms of the Apache License 2.0.

Copyright 2022 [Hyunwoong Ko](https://github.com/hyunwoongko). All Rights Reserved.
