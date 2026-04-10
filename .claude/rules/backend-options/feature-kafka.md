# new-feature: kafka 옵션 — 이벤트 발행/소비

### 이벤트 클래스

```java
// event/{Entity}CreatedEvent.java
public record {Entity}CreatedEvent(
    Long {entity}Id,
    String name,
    String timestamp
) {}
```

### Producer

```java
// event/{Entity}EventProducer.java
@Component
@RequiredArgsConstructor
public class {Entity}EventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish{Entity}Created({Entity}CreatedEvent event) {
        kafkaTemplate.send("{entity}-created", String.valueOf(event.{entity}Id()), event);
    }
}
```

### Consumer (다른 서비스에서 수신 시)

```java
// event/{Entity}EventConsumer.java
@Component
@RequiredArgsConstructor
@Slf4j
public class {Entity}EventConsumer {

    @KafkaListener(topics = "{entity}-created", groupId = "${spring.application.name}")
    public void handle{Entity}Created({Entity}CreatedEvent event) {
        log.info("{Entity} 생성 이벤트 수신: {}", event.{entity}Id());
        // 후속 처리 로직
    }
}
```

### ServiceImpl에 Producer 주입

```java
// create() 메서드 끝에 추가
{entity}EventProducer.publish{Entity}Created(
    new {Entity}CreatedEvent(saved.getId(), saved.getName(), LocalDateTime.now().toString())
);
```

### 추가 생성 파일

```
event/
├── {Entity}CreatedEvent.java
└── {Entity}EventProducer.java
```
