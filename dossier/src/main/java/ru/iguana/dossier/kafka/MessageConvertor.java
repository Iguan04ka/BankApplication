package ru.iguana.dossier.kafka;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class MessageConvertor {
    public String getAddress(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject.optString("address", "");
    }

    public String getTheme(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject.optString("theme", "");
    }

    public String getStatementId(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject.optString("statementId", "");
    }

    public String getText(String jsonString) {
        JSONObject jsonObject = new JSONObject(jsonString);
        return jsonObject.optString("text", "");
    }
}
