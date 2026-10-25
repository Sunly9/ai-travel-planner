package org.example.travel_project1;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
// Spring Boot 4 把测试自动配置挪到了模块化包下
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 接口冒烟测试：跑的是完整的 Spring 上下文 + H2 内存库，不依赖本地 MySQL
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    // Spring Boot 4 默认用 Jackson 3，Spring 容器里没有 Jackson 2 的 ObjectMapper bean，
    // 这里直接用项目依赖的 Jackson 2 构造一个即可
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("公开接口可直接访问")
    void publicHelloWorks() throws Exception {
        mockMvc.perform(get("/api/travel/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("hello world"));
    }

    @Test
    @DisplayName("未带 token 访问受保护接口返回 401")
    void protectedApiRequiresToken() throws Exception {
        mockMvc.perform(get("/api/plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("注册 → 拿 token → 访问我的行程")
    void registerThenAccessPlans() throws Exception {
        String token = register("sm_" + shortId());

        mockMvc.perform(get("/api/plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("重复用户名注册返回 400 且带中文提示")
    void duplicateRegisterReturns400() throws Exception {
        String username = "dup_" + shortId();
        register(username);

        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "test123456",
                "nickname", "重复"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已被占用"));
    }

    @Test
    @DisplayName("参数校验失败返回 400")
    void validationErrorReturns400() throws Exception {
        String body = "{\"username\":\"ab\",\"password\":\"123\",\"nickname\":\"\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("登录后能保存并获得行程")
    void savePlanAndReadBack() throws Exception {
        String token = register("pl_" + shortId());

        String planJson = "{\"success\":true,\"city\":\"北京\",\"days\":1,\"totalBudget\":1000}";
        String body = objectMapper.writeValueAsString(Map.of(
                "city", "北京",
                "days", 1,
                "budget", 1000,
                "planData", planJson));

        mockMvc.perform(post("/api/plans")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].city").value("北京"));
    }

    /**
     * 9 位随机 id：用户名上限 20 字符，nanoTime() 有 17-19 位，拼上前缀会超长
     */
    private String shortId() {
        return String.format("%09d", new java.util.Random().nextInt(1_000_000_000));
    }

    private String register(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "test123456",
                "nickname", "测试"));
        String resp = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(resp);
        return root.path("data").path("token").asText();
    }
}
