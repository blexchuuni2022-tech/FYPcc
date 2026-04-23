package learninggameapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import learninggameapp.ServiceAi;
@RestController
public class QuestionController {
    @Autowired
    private ServiceAi serviceAi;

    // URL: http://localhost:8080/api/question?difficulty=Easy
    @GetMapping("/api/question")
    public String getQuestion(
        @RequestParam(name = "difficulty", defaultValue = "Easy") String difficulty
    ) {
        return serviceAi.generateQuestion(difficulty);
    }

    @GetMapping("/api/question/git")
    @ResponseBody
    public String HiPage() {
        return "hello world git";
    
    }
    @GetMapping("/api/addition")
    public String getAddition(
        @RequestParam(name = "difficulty", defaultValue = "Easy") String difficulty
    ) {
        return serviceAi.additionQuestion(difficulty);
    }

    @GetMapping("/api/subtraction")
    public String getSubtraction(
        @RequestParam(name = "difficulty", defaultValue = "Easy") String difficulty
    ) {
        return serviceAi.subtractionQuestion(difficulty);
    }
@PostMapping("/api/addition/resultanalysis")
@CrossOrigin(origins = "http://localhost:3000")
public String analyseAdditionResults(@RequestBody List<Map<String, Object>> questionLog) {
    return serviceAi.analyseAdditionResults(questionLog);
}

@PostMapping("/api/subtraction/resultanalysis")
@CrossOrigin(origins = "http://localhost:3000")
public String analyseSubtractionResults(@RequestBody List<Map<String, Object>> questionLog) {
    return serviceAi.analyseSubtractionResults(questionLog);
}

@PostMapping("/api/multiplication/resultanalysis")
@CrossOrigin(origins = "http://localhost:3000")
public String analyseMultiplicationResults(@RequestBody List<Map<String, Object>> questionLog) {
    return serviceAi.analyseMultiplicationResults(questionLog);
}

@PostMapping("/api/multiplication/adaptive")
public String getAdaptiveMultiplication(@RequestBody Map<String, Object> payload) {
    String difficulty = (String) payload.get("difficulty");

    List<Map<String, Object>> history = (List<Map<String, Object>>) payload.get("history");
    System.out.println(history);
    return serviceAi.multiplicationQuestion(difficulty, history);
}
@PostMapping("api/diagnose")
@CrossOrigin(origins = "http://localhost:3000")
public String diagnose(@RequestBody Map<String, Object> payload) {
    List<Map<String, Object>> history = (List<Map<String, Object>>) payload.get("history");
    String currentLevel = (String) payload.get("currentLevel");
    return serviceAi.generateAdaptiveInstruction(history);
}

    @GetMapping("/api/testai")
    public String getTestai(
        @RequestParam(name = "difficulty", defaultValue = "Easy") String difficulty
    ) {
        return serviceAi.testAiLogic();
    }


}
