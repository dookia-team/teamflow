# test: kafka 옵션 — 이벤트 테스트

```java
// test/.../event/{Entity}EventTest.java
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"{entity}-created"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:9092"}
)
class {Entity}EventTest {

    @Autowired private {Entity}EventProducer producer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("이벤트 발행 → 정상 전송")
    void publishEvent_success() {
        var event = new {Entity}CreatedEvent(1L, "테스트", LocalDateTime.now().toString());

        assertThatCode(() -> producer.publish{Entity}Created(event))
            .doesNotThrowAnyException();
    }
}
```

### 테스트 파일 위치

```
src/test/java/com/{company}/{app}/event/
└── {Entity}EventTest.java          ← @EmbeddedKafka
```
