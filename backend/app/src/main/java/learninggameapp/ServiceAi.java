package learninggameapp;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import dev.langchain4j.model.ollama.OllamaChatModel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
@Service
public class ServiceAi {

    private final ChatLanguageModel model;
    private final Random random = new Random();
    private final ObjectMapper mapper = new ObjectMapper();
    


  public ServiceAi() {
        // We replace GoogleAiGeminiChatModel with OllamaChatModel
        this.model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("gemma3:4b")
                .temperature(0.9)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    public String generateQuestion(String difficulty) {
        String prompt = "Generate a math question for a primary school student. " + "the question needs to be integer" + " "+
                        "seed: "+ random.nextLong()  + ". "+
                        "Difficulty: " + difficulty + ". " +
                        "Format: JSON with fields 'question', 'options', 'answer'.";
        
        return model.generate(prompt); 
    }

public String additionQuestion(String difficulty) {
    String mode = difficulty.toLowerCase();
    if (mode.equals("1")) mode = "easy";
    if (mode.equals("2")) mode = "normal";
    if (mode.equals("3")) mode = "hard";

    String digitRule, example;
    
    switch (mode) {
        case "easy":
            digitRule = "num1 is 2 digits (10-99), num2 is 2 digits (10-99), NO carrying required (ones digits must sum to 9 or less)";
            example = "e.g. 23+15=38, 41+32=73, 54+25=79";
            break;
        case "hard":
            digitRule = "num1 is 3 digits (100-999), num2 is 3 digits (100-999), carrying REQUIRED";
            example = "e.g. 347+285=632, 463+179=642";
            break;
        default:
            digitRule = "num1 is 2 digits (20-99), num2 is 2 digits (10-99), carrying REQUIRED (ones digits must sum to 10 or more)";
            example = "e.g. 47+35=82, 68+54=122, 73+49=122";
    }

    long seed = random.nextLong();

    String prompt = """
        You are a math question generator for a primary school game.
        
        DIFFICULTY: %s
        RULE: %s
        EXAMPLE: %s
        SEED (use this to pick different numbers each time): %d
        
        IMPORTANT:
        1. Follow the digit rule strictly
        2. Write a short encouraging explanation (1 sentence, plain text)
        
        RESPOND WITH ONLY THIS JSON (no markdown, no backticks):
        {"num1": <integer>, "num2": <integer>, "explanation": "<text>"}
        """.formatted(mode.toUpperCase(), digitRule, example, seed);

    int attempts = 0;
    while (attempts < 5) {
        attempts++;
        try {
            String response = model.generate(prompt);
            response = response.replaceAll("```json|```", "").trim();
            if (response.contains("{")) {
                response = response.substring(response.indexOf("{"),
                           response.lastIndexOf("}") + 1);
            }

            JsonNode root = mapper.readTree(response);
            int num1 = root.get("num1").asInt();
            int num2 = root.get("num2").asInt();
            String explanation = root.has("explanation")
                               ? root.get("explanation").asText()
                               : "Can you solve this addition?";

            // Java validates ranges
            boolean valid = true;
            if (mode.equals("easy")   && (num1 < 10 || num1 > 99 || num2 < 10 || num2 > 99)) valid = false;
            if (mode.equals("normal") && (num1 < 20 || num1 > 99 || num2 < 10 || num2 > 99)) valid = false;
            if (mode.equals("hard")   && (num1 < 100 || num1 > 999 || num2 < 100 || num2 > 999)) valid = false;

            // Validate carry rule
            if (mode.equals("easy") && (num1 % 10 + num2 % 10) > 9) valid = false;
            if (mode.equals("normal") && (num1 % 10 + num2 % 10) < 10) valid = false;

            if (!valid) {
                System.out.println("Addition attempt " + attempts + ": invalid, retrying...");
                continue;
            }

            // Java always computes correct result
            int result = num1 + num2;

            // Pad num2 to match num1 digit length
            int num1Digits = String.valueOf(num1).length();
            int num2Digits = String.valueOf(num2).length();
            int zerosNeeded = num1Digits - num2Digits;

            ObjectNode out = mapper.createObjectNode();
            out.set("num1", intToArray(num1));

            if (zerosNeeded > 0) {
                ArrayNode padded = mapper.createArrayNode();
                for (int i = 0; i < zerosNeeded; i++) padded.add(0);
                String v2 = String.valueOf(num2);
                for (char c : v2.toCharArray()) padded.add(Character.getNumericValue(c));
                out.set("num2", padded);
            } else {
                out.set("num2", intToArray(num2));
            }

            out.set("result", intToArray(result));
            out.put("operator", "+");
            out.put("explanation", explanation);
            return out.toString();

        } catch (Exception e) {
            System.out.println("Addition attempt " + attempts + " failed: " + e.getMessage());
        }
    }

    return "{\"num1\":[2,3], \"num2\":[1,5], \"result\":[3,8], \"operator\":\"+\", \"explanation\":\"23 plus 15 equals 38!\"}";
}

public String subtractionQuestion(String difficulty) {
    String mode = difficulty.toLowerCase();
    if (mode.equals("1")) mode = "easy";
    if (mode.equals("2")) mode = "normal";
    if (mode.equals("3")) mode = "hard";

    String digitRule, example;
    switch (mode) {
        case "easy":
            digitRule = "num1 must be between 1-20, num2 must be a single digit (1-9), NO borrowing required, result must be positive";
            example = "e.g. 15-3=12, 8-5=3, 17-4=13";
            break;
        case "hard":
            digitRule = "num1 must be 3 digits (100-999), num2 must be 2 or 3 digits, borrowing REQUIRED";
            example = "e.g. 432-158=274, 701-246=455";
            break;
        default: // normal
            digitRule = "num1 must be 2 digits (20-99), num2 must be 2 digits (10-99), borrowing REQUIRED, result must be positive";
            example = "e.g. 53-27=26, 82-45=37";
    }

    long seed = random.nextLong(); // forces AI to think differently each call

    String prompt = """
        You are a math question generator for a primary school game.
        
        DIFFICULTY: %s
        RULE: %s
        EXAMPLE: %s
        SEED (use this to pick different numbers each time): %d
        
        IMPORTANT:
        1. num1 must be GREATER than num2
        2. result must be POSITIVE
        3. Follow the digit rule strictly
        4. Write a short encouraging explanation (1 sentence, plain text)
        
        RESPOND WITH ONLY THIS JSON (no markdown, no backticks):
        {"num1": <integer>, "num2": <integer>, "explanation": "<text>"}
        """.formatted(mode.toUpperCase(), digitRule, example, seed);

    int attempts = 0;
    while (attempts < 5) {
        attempts++;
        try {
            String response = model.generate(prompt);
            response = response.replaceAll("```json|```", "").trim();
            if (response.contains("{")) {
                response = response.substring(response.indexOf("{"),
                           response.lastIndexOf("}") + 1);
            }

            JsonNode root = mapper.readTree(response);
            int num1 = root.get("num1").asInt();
            int num2 = root.get("num2").asInt();
            String explanation = root.has("explanation")
                               ? root.get("explanation").asText()
                               : "Can you solve this subtraction?";

            // Java validates and fixes if AI broke the rules
            if (num2 > num1) { int temp = num1; num1 = num2; num2 = temp; }

            // Enforce digit ranges per level
            boolean valid = true;
            if (mode.equals("easy")   && (num1 > 20 || num1 < 1))  valid = false;
            if (mode.equals("normal") && (num1 < 20 || num1 > 99)) valid = false;
            if (mode.equals("hard")   && (num1 < 100 || num1 > 999)) valid = false;

            if (!valid) {
                System.out.println("Attempt " + attempts + ": AI gave out-of-range numbers, retrying...");
                continue;
            }

            // Java always computes the correct result
            int result = num1 - num2;
            if (result <= 0) continue; // retry if result is 0 or negative

            // Java builds the final JSON with proper padding
            ObjectNode out = mapper.createObjectNode();

            // Pad num2 to match num1's digit length
            if (num1 >= 100 && num2 < 100) {
                ArrayNode padded = mapper.createArrayNode();
                if (num2 < 10) padded.add(0);
                padded.add(0);
                String v2 = String.valueOf(num2);
                for (char c : v2.toCharArray()) padded.add(Character.getNumericValue(c));
                out.set("num1", intToArray(num1));
                out.set("num2", padded);
            } else if (num1 >= 10 && num2 < 10) {
                ArrayNode padded = mapper.createArrayNode();
                padded.add(0);
                padded.add(num2);
                out.set("num1", intToArray(num1));
                out.set("num2", padded);
            } else {
                out.set("num1", intToArray(num1));
                out.set("num2", intToArray(num2));
            }

            out.set("result", intToArray(result));
            out.put("operator", "-");
            out.put("explanation", explanation);
            return out.toString();

        } catch (Exception e) {
            System.out.println("Attempt " + attempts + " failed: " + e.getMessage());
        }
    }

    // Fallback
    return "{\"num1\":[1,5], \"num2\":[0,3], \"result\":[1,2], \"operator\":\"-\", \"explanation\":\"15 minus 3 equals 12!\"}";
}
public String multiplicationQuestion(String difficulty, List<Map<String, Object>> history) {
    String mode = difficulty.toLowerCase();
    if (mode.equals("1")) mode = "easy";
    if (mode.equals("2")) mode = "normal";
    if (mode.equals("3")) mode = "hard";

    String digitRule, example;
    switch (mode) {
        case "easy":
            digitRule = "num1 is a single digit (1-9), num2 is a single digit (1-9), result MUST be 1 digit (1-9). e.g. 2×3=6, 1×5=5, 3×2=6";
            example = "VALID: 2×3=6, 1×5=5. INVALID: 3×7=21 (result is 2 digits!)";
            break;
        case "hard":
            digitRule = "num1 is 2 digits (10-99), num2 is 2 digits (10-19), result MUST be 3 digits (100-999). e.g. 12×9=108, 25×5=125";
            example = "VALID: 12×9=108, 25×4=100. INVALID: 5×6=30 (result is 2 digits!)";
            break;
        default: // normal
            digitRule = "num1 is a single digit (2-9), num2 is a single digit (2-9), result MUST be 2 digits (10-99). e.g. 5×6=30, 9×9=81, 7×8=56";
            example = "VALID: 5×6=30, 9×9=81. INVALID: 2×3=6 (result is 1 digit!), 3×7=21 is OK";
    }

    long seed = random.nextLong();
    String historyStr = history.isEmpty() ? "None" : history.toString();

    String prompt = """
        You are a math question generator for a primary school game.
        
        DIFFICULTY: %s
        RULE: %s
        EXAMPLE: %s
        HISTORY (do NOT reuse these pairs or their reverse): %s
        SEED (use this to pick different numbers each time): %d
        
        IMPORTANT:
        1. Follow the digit rule strictly — check your result digit count before answering
        2. Do NOT reuse any pair from history
        3. Write a short encouraging explanation (1 sentence, plain text)
        
        RESPOND WITH ONLY THIS JSON (no markdown, no backticks):
        {"num1": <integer>, "num2": <integer>, "explanation": "<text>"}
        """.formatted(mode.toUpperCase(), digitRule, example, historyStr, seed);

    int attempts = 0;
    while (attempts < 5) {
        attempts++;
        try {
            String response = model.generate(prompt);
            response = response.replaceAll("```json|```", "").trim();
            if (response.contains("{")) {
                response = response.substring(response.indexOf("{"),
                           response.lastIndexOf("}") + 1);
            }

            JsonNode root = mapper.readTree(response);
            int num1 = root.get("num1").asInt();
            int num2 = root.get("num2").asInt();
            String explanation = root.has("explanation")
                               ? root.get("explanation").asText()
                               : "Can you solve this one?";

            // Java validates digit count
            int result = num1 * num2;
            int digits = String.valueOf(result).length();
            int expectedDigits = mode.equals("easy") ? 1 : mode.equals("hard") ? 3 : 2;

            boolean valid = true;
            if (digits != expectedDigits) valid = false;
            if (num1 <= 0 || num2 <= 0) valid = false;

            if (!valid) {
                System.out.println("Attempt " + attempts + ": " + num1 + "×" + num2
                    + "=" + result + " invalid, retrying...");
                continue;
            }

            // Java always computes correct result
            ObjectNode out = mapper.createObjectNode();
            out.set("num1", intToArray(num1));
            out.set("num2", intToArray(num2));
            out.set("result", intToArray(result));
            out.put("operator", "x");
            out.put("explanation", explanation);
            return out.toString();

        } catch (Exception e) {
            System.out.println("Multiplication attempt " + attempts + " failed: " + e.getMessage());
        }
    }

    // Fallback — same as subtraction pattern
    return "{\"num1\":[2],\"num2\":[3],\"result\":[6],\"operator\":\"x\","
         + "\"explanation\":\"2 times 3 equals 6 — great start!\"}";
}



public String analyseAdditionResults(List<Map<String, Object>> questionLog) {
    String prompt = """
        You are a friendly and encouraging primary school math teacher.
        A student just completed 12 addition questions. Here is their full result log:
        
        %s
        
        Each entry contains:
        - questionNum, num1, num2, result, correct, timeTaken, attemptsUsed, level (1=Easy 2=Medium 3=Hard)
        
        Please analyse and respond with ONLY this JSON (no markdown, no backticks):
        {
          "overall": "<1 sentence overall comment>",
          "strength": "<what they did well>",
          "weakness": "<what they struggled with>",
          "tip": "<one specific study tip>",
          "encouragement": "<fun motivating closing sentence for a child>"
        }
        """.formatted(questionLog.toString());

    int attempts = 0;
    while (attempts < 3) {
        attempts++;
        try {
            String response = model.generate(prompt);
            response = response.replaceAll("```json|```", "").trim();
            if (response.contains("{")) {
                response = response.substring(response.indexOf("{"),
                           response.lastIndexOf("}") + 1);
            }
            mapper.readTree(response);
            return response;
        } catch (Exception e) {
            System.out.println("Addition analysis attempt " + attempts + " failed: " + e.getMessage());
        }
    }
    return "{\"overall\":\"Great effort completing all 12 questions!\","
         + "\"strength\":\"You kept trying even when it got hard.\","
         + "\"weakness\":\"Some questions took a few attempts.\","
         + "\"tip\":\"Practice carrying numbers with 2-digit addition every day.\","
         + "\"encouragement\":\"You are a math superstar in the making!\"}";
}

public String analyseSubtractionResults(List<Map<String, Object>> questionLog) {
    String prompt = """
        You are a friendly and encouraging primary school math teacher.
        A student just completed 12 subtraction questions. Here is their full result log:
        
        %s
        
        Each entry contains:
        - questionNum: question number
        - num1, num2: the numbers in the question (num1 - num2)
        - result: the correct answer
        - correct: whether the student got it right
        - timeTaken: how many seconds they took
        - attemptsUsed: how many attempts they used (max 3)
        - level: 1=Easy, 2=Medium, 3=Hard
        
        Please analyse the student's performance and provide:
        1. OVERALL: A warm 1-sentence overall comment on their performance
        2. STRENGTH: What they did well (e.g. fast on easy questions, good at borrowing)
        3. WEAKNESS: What they struggled with (e.g. slow on hard questions, needed multiple attempts)
        4. TIP: One specific, actionable study tip for the student to improve
        5. ENCOURAGEMENT: A fun, motivating closing sentence for a child
        
        RESPOND WITH ONLY THIS JSON (no markdown, no backticks):
        {
          "overall": "<text>",
          "strength": "<text>",
          "weakness": "<text>",
          "tip": "<text>",
          "encouragement": "<text>"
        }
        """.formatted(questionLog.toString());

    int attempts = 0;
    while (attempts < 3) {
        attempts++;
        try {
            String response = model.generate(prompt);
            response = response.replaceAll("```json|```", "").trim();
            if (response.contains("{")) {
                response = response.substring(response.indexOf("{"),
                           response.lastIndexOf("}") + 1);
            }
            // Validate it's parseable JSON
            mapper.readTree(response);
            return response;
        } catch (Exception e) {
            System.out.println("Analysis attempt " + attempts + " failed: " + e.getMessage());
        }
    }

    

    // Fallback
    return "{\"overall\":\"Great effort completing all 12 questions!\","
         + "\"strength\":\"You kept trying even when it got hard.\","
         + "\"weakness\":\"Some questions took a few attempts.\","
         + "\"tip\":\"Practice borrowing with 2-digit numbers every day.\","
         + "\"encouragement\":\"You are a math superstar in the making!\"}";
}

public String analyseMultiplicationResults(List<Map<String, Object>> questionLog) {
    String prompt = """
        You are a friendly and encouraging primary school math teacher.
        A student just completed 12 multiplication questions. Here is their full result log:
        
        %s
        
        Each entry contains:
        - questionNum, num1, num2, result, correct, timeTaken, attemptsUsed, level (1=Easy 2=Medium 3=Hard)
        
        Please analyse and respond with ONLY this JSON (no markdown, no backticks):
        {
          "overall": "<1 sentence overall comment>",
          "strength": "<what they did well>",
          "weakness": "<what they struggled with>",
          "tip": "<one specific study tip>",
          "encouragement": "<fun motivating closing sentence for a child>"
        }
        """.formatted(questionLog.toString());

    int attempts = 0;
    while (attempts < 3) {
        attempts++;
        try {
            String response = model.generate(prompt);
            response = response.replaceAll("```json|```", "").trim();
            if (response.contains("{")) {
                response = response.substring(response.indexOf("{"),
                           response.lastIndexOf("}") + 1);
            }
            mapper.readTree(response);
            return response;
        } catch (Exception e) {
            System.out.println("Multiplication analysis attempt " + attempts + " failed: " + e.getMessage());
        }
    }
    return "{\"overall\":\"Great effort completing all 12 questions!\","
         + "\"strength\":\"You showed great determination throughout.\","
         + "\"weakness\":\"Some times tables need more practice.\","
         + "\"tip\":\"Try reciting the 7 and 8 times tables every morning.\","
         + "\"encouragement\":\"Keep going — you are getting stronger every day!\"}";
}

public String testAiLogic(){
    String prompt = """
    Act as a Diagnostic Math Tutor for Primary School students. 
    Your goal is to generate ONE unique multiplication problem based on the user's history.
    
    ANALYSIS LOGIC:
    1. Review the user's history of incorrect answers.
    2. Identify patterns in mistakes (e.g., struggling with the 7-times table or 8-times table).
    3. For the HARD level, you must generate a question that specifically targets these identified weaknesses to provide remedial practice.
    4. Ensure the new question is similar in logic but NOT identical to a previously failed one.

    STRICT LEVEL CONSTRAINTS:
    - EASY (Mode 1): Use simple single-digit multiplication. 
      Target pairs: 2x5, 3x3, 4x2, etc. (Generally numbers 1-5).
      
    - NORMAL (Mode 2): Use standard primary school multiplication tables (up to 10x10).
      Target pairs: 9x6, 8x5, 7x4, etc. Focus on the "heavier" tables that require better memorization.
      
    - HARD (Mode 3): This level is strictly ADAPTIVE. 
      Analyze the user's answer history. If they keep getting questions wrong, generate "Mirror" questions (e.g., if they fail 7x8, try 8x7 or 7x9) to reinforce the specific concept they are stuck on.
    now is the user input : 
    2 x 5 = 10, 3 x 3 = 9 , 5 x 4 = 15, 6 x 3 = 12, 4 x 2 = 8.
    """;
                

   try {
        String response = model.generate(prompt);
        // Standard JSON cleaning logic
        if (response.contains("```json")) {
            response = response.substring(response.indexOf("```json") + 7, response.lastIndexOf("```"));
        }

        System.out.println(response);
        return response.trim();
    } catch (Exception e) {
        return "{\"num1\":[4], \"num2\":[2], \"result\":[8], \"operator\":\"x\", \"explanation\":\"Let's try 4 times 2!\"}";
    }
}

private String generateAIExplanation(int val1, int val2, String op, String type) {
    try {
        // AI only generates the child-friendly text
        String prompt = "Act as a Math Teacher. Explain how to solve " + val1 + " " + op + " " + val2 + 
                        ". Keep it simple for a child. Output plain text only.";
        String explanation = model.generate(prompt);
        
        if (explanation.length() > 200) explanation = explanation.substring(0, 200) + "...";

        ObjectNode root = mapper.createObjectNode();
        root.set("num1", intToArray(val1));
        
        // Vertical Alignment Logic (Padding)
        if (val2 < 10 && val1 >= 10) {
             ArrayNode padded = mapper.createArrayNode();
             padded.add(0); padded.add(val2);
             root.set("num2", padded);
        } else {
             root.set("num2", intToArray(val2));
        }

        // --- THE FIX: Calculate and Add Result ---
        int res;
        if (op.equals("+")) res = val1 + val2;
        else if (op.equals("-")) res = val1 - val2;
        else res = val1 * val2; // Multiplication case

        root.set("result", intToArray(res));
        root.put("operator", op);  
        root.put("explanation", explanation);

        return root.toString();
    } catch (Exception e) {
        return "{\"error\":\"json_error\"}";
    }
}

    private int extractInt(JsonNode node) {
        if (node == null) return 0;
        
        if (node.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode digit : node) {
                sb.append(digit.asText());
            }
            if (sb.length() == 0) return 0;
            return Integer.parseInt(sb.toString());
        }
        

        return Integer.parseInt(node.asText());
    }

public String generateAdaptiveInstruction(List<Map<String, Object>> history) {
    String prompt = "Review these 5 math attempts: " + history.toString() + ".\n" +
                    "Identify the specific mathematical weakness (e.g., 'struggling with multiplication involving 8' or 'multi-digit alignment').\n" +
                    "Provide a supportive 1-sentence tip for a child.\n" +
                    "Decide the next Level (1=Easy, 2=Normal, 3=Hard).\n" +
                    "Output ONLY JSON: {\"action\": \"level_number\", \"tip\": \"text\"}";

    String response = model.generate(prompt);
    return response.replace("```json", "").replace("```", "").trim();
}




    private int parseLevel(String difficulty) {
        try {
            if (difficulty == null) return 2;
            if (difficulty.equalsIgnoreCase("Easy")) return 1;
            if (difficulty.equalsIgnoreCase("Hard")) return 3;
            return Integer.parseInt(difficulty);
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    private ArrayNode intToArray(int value) {
        ArrayNode arr = mapper.createArrayNode();
        String str = String.valueOf(value);
        for (char c : str.toCharArray()) arr.add(Character.getNumericValue(c));
        return arr;
    }

    
}
    
