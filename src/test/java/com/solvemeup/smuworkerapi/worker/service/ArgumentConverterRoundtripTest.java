package com.solvemeup.smuworkerapi.worker.service;

import com.solvemeup.smuworkerapi.worker.dto.ParameterSpec;
import com.solvemeup.smuworkerapi.worker.enums.ValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArgumentConverter 왕복 변환 정확성 검증
 *
 *   arguments
 *       → toStdinInput()   → stdin
 *       → stdinToArguments() → arguments
 *
 * stdinToArguments()는 WA 발생 시 실패한 테스트 케이스의 인자를
 * 결과 메시지(SubmissionResultMessage.arguments)에 담기 위해 사용됩니다.
 */
class ArgumentConverterRoundtripTest {

    private ArgumentConverter converter;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        converter = new ArgumentConverter();
    }

    // ── 단일 파라미터 왕복 (모두 정상) ──────────────────────────────────────────

    @Test
    @DisplayName("[단일] INT 왕복")
    void single_int() throws Exception {
        assertRoundtrip(
                List.of(spec("n", ValueType.INT)),
                args("42"),
                List.of("42")
        );
    }

    @Test
    @DisplayName("[단일] LONG 왕복")
    void single_long() throws Exception {
        assertRoundtrip(
                List.of(spec("n", ValueType.LONG)),
                args("9999999999"),
                List.of("9999999999")
        );
    }

    @Test
    @DisplayName("[단일] BOOLEAN 왕복")
    void single_boolean() throws Exception {
        assertRoundtrip(
                List.of(spec("b", ValueType.BOOLEAN)),
                args("true"),
                List.of("true")
        );
    }

    @Test
    @DisplayName("[단일] STRING 왕복")
    void single_string() throws Exception {
        assertRoundtrip(
                List.of(spec("s", ValueType.STRING)),
                args("hello"),
                List.of("hello")
        );
    }

    @Test
    @DisplayName("[단일] INT_ARRAY 왕복")
    void single_intArray() throws Exception {
        assertRoundtrip(
                List.of(spec("a", ValueType.INT_ARRAY)),
                args("[1,2,3,4,5]"),
                List.of("[1,2,3,4,5]")
        );
    }

    @Test
    @DisplayName("[단일] LONG_ARRAY 왕복")
    void single_longArray() throws Exception {
        assertRoundtrip(
                List.of(spec("a", ValueType.LONG_ARRAY)),
                args("[1000000000,2000000000,3000000000]"),
                List.of("[1000000000,2000000000,3000000000]")
        );
    }

    @Test
    @DisplayName("[단일] BOOLEAN_ARRAY 왕복")
    void single_booleanArray() throws Exception {
        assertRoundtrip(
                List.of(spec("a", ValueType.BOOLEAN_ARRAY)),
                args("[true,false,true]"),
                List.of("[true,false,true]")
        );
    }

    @Test
    @DisplayName("[단일] STRING_ARRAY 왕복")
    void single_stringArray() throws Exception {
        String inner = mapper.writeValueAsString(new String[]{"apple", "banana", "cherry"});
        assertRoundtrip(
                List.of(spec("a", ValueType.STRING_ARRAY)),
                args(inner),
                List.of("[\"apple\",\"banana\",\"cherry\"]")
        );
    }

    @Test
    @DisplayName("[단일] INT_2D_ARRAY 왕복")
    void single_int2dArray() throws Exception {
        assertRoundtrip(
                List.of(spec("m", ValueType.INT_2D_ARRAY)),
                args("[[1,2],[3,4],[5,6]]"),
                List.of("[[1,2],[3,4],[5,6]]")
        );
    }

    @Test
    @DisplayName("[단일] LONG_2D_ARRAY 왕복")
    void single_long2dArray() throws Exception {
        assertRoundtrip(
                List.of(spec("m", ValueType.LONG_2D_ARRAY)),
                args("[[1000000000,2000000000],[3000000000,4000000000]]"),
                List.of("[[1000000000,2000000000],[3000000000,4000000000]]")
        );
    }

    @Test
    @DisplayName("[단일] BOOLEAN_2D_ARRAY 왕복")
    void single_boolean2dArray() throws Exception {
        assertRoundtrip(
                List.of(spec("m", ValueType.BOOLEAN_2D_ARRAY)),
                args("[[true,false],[false,true]]"),
                List.of("[[true,false],[false,true]]")
        );
    }

    @Test
    @DisplayName("[단일] STRING_2D_ARRAY 왕복")
    void single_string2dArray() throws Exception {
        String inner = mapper.writeValueAsString(new String[][]{{"hello", "world"}, {"foo", "bar"}});
        assertRoundtrip(
                List.of(spec("m", ValueType.STRING_2D_ARRAY)),
                args(inner),
                List.of("[[\"hello\",\"world\"],[\"foo\",\"bar\"]]")
        );
    }

    // ── 복합 파라미터 왕복 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[복합] INT_ARRAY → INT 왕복 (정상)")
    void multi_intArray_then_int() throws Exception {
        // 배열 타입 → 스칼라 타입 순서는 정상 동작
        // scanner.nextLine()으로 배열을 읽은 뒤 scanner.next()로 스칼라를 읽으므로 문제없음
        assertRoundtrip(
                List.of(spec("nums", ValueType.INT_ARRAY), spec("target", ValueType.INT)),
                mapper.writeValueAsString(new String[]{"[2,7,11,15]", "9"}),
                List.of("[2,7,11,15]", "9")
        );
    }

    @Test
    @DisplayName("[복합] INT_ARRAY → INT_ARRAY 왕복 (정상)")
    void multi_intArray_then_intArray() throws Exception {
        assertRoundtrip(
                List.of(spec("a", ValueType.INT_ARRAY), spec("b", ValueType.INT_ARRAY)),
                mapper.writeValueAsString(new String[]{"[1,2,3]", "[4,5,6]"}),
                List.of("[1,2,3]", "[4,5,6]")
        );
    }

    @Test
    @DisplayName("[복합] INT → INT 왕복 (정상)")
    void multi_int_then_int() throws Exception {
        // scanner.next()는 공백/개행을 건너뛰므로 스칼라 연속은 정상
        assertRoundtrip(
                List.of(spec("a", ValueType.INT), spec("b", ValueType.INT)),
                mapper.writeValueAsString(new String[]{"10", "20"}),
                List.of("10", "20")
        );
    }

    @Test
    @DisplayName("[복합] INT → INT_ARRAY 왕복 (정상)")
    void multi_int_then_intArray() throws Exception {
        // scanner.next()로 INT를 읽은 후 scanner.nextLine()으로 INT_ARRAY를 읽는 조합
        // Scanner가 내부적으로 줄 위치를 추적하므로 올바르게 동작함
        assertRoundtrip(
                List.of(spec("base", ValueType.INT), spec("arr", ValueType.INT_ARRAY)),
                mapper.writeValueAsString(new String[]{"10", "[1,2,3]"}),
                List.of("10", "[1,2,3]")
        );
    }

    @Test
    @DisplayName("[복합] BOOLEAN → STRING_ARRAY 왕복 (정상)")
    void multi_boolean_then_stringArray() throws Exception {
        String inner = mapper.writeValueAsString(new String[]{"apple", "banana"});
        assertRoundtrip(
                List.of(spec("flag", ValueType.BOOLEAN), spec("words", ValueType.STRING_ARRAY)),
                mapper.writeValueAsString(new String[]{"true", inner}),
                List.of("true", "[\"apple\",\"banana\"]")
        );
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────────

    private ParameterSpec spec(String name, ValueType type) {
        return new ParameterSpec(name, type);
    }

    private String args(String argValue) throws Exception {
        return mapper.writeValueAsString(new String[]{argValue});
    }

    private void assertRoundtrip(
            List<ParameterSpec> params,
            String arguments,
            List<String> expectedArguments
    ) {
        String stdin = converter.toStdinInput(arguments, params);
        List<String> result = converter.stdinToArguments(stdin, params);
        assertThat(result).isEqualTo(expectedArguments);
    }
}
