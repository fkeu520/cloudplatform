package com.cloudhub.platform.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局序列化配置
 *
 * <p>核心目的: <b>防止 Snowflake ID (19 位) 在前端 JavaScript Number 类型下精度丢失</b>
 *
 * <p>Java Long 最大 2^63-1 = 9223372036854775807 (19 位)。
 * JavaScript Number 最大安全整数 = 2^53-1 = 9007199254740991 (16 位)。
 * 超过安全整数范围, JS 会丢精度 (e.g. 2069257993972027393 → 2069257993972027400)。
 *
 * <p>方案: 给 {@code Long.class} 注册 {@link ToStringSerializer},
 * 强制所有 Long 字段序列化为 JSON 字符串。
 * 与 entity 上的 {@code @JsonFormat(shape = STRING)} 行为一致 —
 * 后者在 Lombok @Data 生成 public getter 时, Jackson 可能走 getter 路径而忽略
 * private field 上的 @JsonFormat, 因此全局注册兜底更可靠。
 *
 * <p>前端必须:
 * <ul>
 *   <li>接收时用 string 类型 (e.g. id: string)</li>
 *   <li>比较时用 {@code String(scope.row.id)} 而非直接 ===</li>
 *   <li>请求时用 string (后端 Spring MVC 自动 String → Long 转换)</li>
 * </ul>
 *
 * <p>不影响:
 * <ul>
 *   <li>{@code Integer} / {@code Double} / {@code BigDecimal} → 仍按数字输出 (只针对 Long)</li>
 *   <li>{@code LocalDateTime} → 由 Spring Boot 默认行为控制</li>
 * </ul>
 *
 * <p>注: 不用 {@code JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS} — 那个会把所有数字类型
 * (Long/Integer/Double) 都转字符串, 破坏现有 floorNumber/sorting 等整型字段。
 *
 * @author Sisyphus
 * @since 2026-06-25
 */
@Configuration
public class JacksonConfig {

    /**
     * 全局 Long → JSON String 序列化.
     * <p>所有服务 (依赖 platform-common) 自动继承, 无需每个 application.yml 重复配置.
     * <p>同时处理 {@code Long} 包装类和 {@code long} 基本类型 (通过 {@code long.class}
     * 单独注册, 因为 {@code Long.TYPE != Long.class}).
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsStringCustomizer() {
        return builder -> builder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
