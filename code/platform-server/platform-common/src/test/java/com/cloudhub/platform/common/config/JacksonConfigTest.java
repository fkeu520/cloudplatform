package com.cloudhub.platform.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JacksonConfig 单元测试
 *
 * <h2>覆盖</h2>
 * <ul>
 *   <li>TC-JC-01: WRITE_LONG_AS_STRING 已被开启 (防止 19 位 Snowflake ID 在前端 JS Number 丢精度)</li>
 *   <li>TC-JC-02: Long 字段实际序列化为 JSON string (不是 number)</li>
 *   <li>TC-JC-03: Integer / String 字段不受影响 (仍按原类型输出)</li>
 * </ul>
 *
 * <h2>背景</h2>
 * <p>Java Long 最大 2^63-1 (19 位), JS Number.MAX_SAFE_INTEGER = 2^53-1 (16 位)。
 * 超安全整数 JS 会丢精度 (e.g. 2069257993972027393 → 2069257993972027400)。
 * 必须 Long → JSON string。
 */
@DisplayName("Jackson 全局 Long→String 序列化测试 (3 TC)")
@JsonTest
@Import(JacksonConfig.class)
class JacksonConfigTest {

    /** Snowflake ID 测试值 (19 位, 超过 JS Number.MAX_SAFE_INTEGER) */
    private static final long SNOWFLAKE_ID = 2069257993972027393L;

    @Autowired
    private JacksonTester<SnowflakeEntity> json;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("TC-JC-01: WRITE_LONG_AS_STRING 特性已开启")
    void writeLongAsStringFeature_enabled() throws Exception {
        SerializerProvider provider = objectMapper.getSerializerProviderInstance();
        assertThat(provider.findValueSerializer(Long.class).getClass())
                .as("Long.class 必须注册 ToStringSerializer (防止 19 位 Snowflake 精度丢失)")
                .isEqualTo(ToStringSerializer.class);
        assertThat(provider.findValueSerializer(Long.TYPE).getClass())
                .as("long 基本类型也必须注册 ToStringSerializer")
                .isEqualTo(ToStringSerializer.class);
    }

    @Test
    @DisplayName("TC-JC-02: Long 字段序列化为 JSON string (无精度丢失)")
    void longField_serializedAsString() throws Exception {
        SnowflakeEntity entity = new SnowflakeEntity();
        entity.setId(SNOWFLAKE_ID);
        entity.setParkId(SNOWFLAKE_ID);
        entity.setName("科技园A座");

        // 验证 JSON 包含 "id":"2069257993972027393" 而非 "id":2069257993972027400
        assertThat(json.write(entity))
                .extractingJsonPathStringValue("id")
                .isEqualTo("2069257993972027393");
        assertThat(json.write(entity))
                .extractingJsonPathStringValue("parkId")
                .isEqualTo("2069257993972027393");
    }

    @Test
    @DisplayName("TC-JC-03: String / Integer 字段不受影响 (按原类型输出)")
    void otherTypes_unaffected() throws Exception {
        SnowflakeEntity entity = new SnowflakeEntity();
        entity.setId(SNOWFLAKE_ID);
        entity.setFloorNumber(10);  // Integer
        entity.setName("A 座");     // String

        assertThat(json.write(entity))
                .extractingJsonPathStringValue("name")
                .isEqualTo("A 座");
        // floorNumber 是 Integer, 不受 WRITE_LONG_AS_STRING 影响, 仍输出为 number
        assertThat(json.write(entity))
                .extractingJsonPathNumberValue("floorNumber")
                .isEqualTo(10);
    }

    /** 测试用实体 */
    static class SnowflakeEntity {
        private Long id;
        private Long parkId;
        private String name;
        private Integer floorNumber;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getParkId() { return parkId; }
        public void setParkId(Long parkId) { this.parkId = parkId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getFloorNumber() { return floorNumber; }
        public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
    }
}
