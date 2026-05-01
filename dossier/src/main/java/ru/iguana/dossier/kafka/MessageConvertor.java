package ru.iguana.dossier.kafka;

import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Service
public class MessageConvertor {

    private static final DecimalFormatSymbols RU_SYMBOLS = buildRuSymbols();

    public String getAddress(String jsonString) {
        return new JSONObject(jsonString).optString("address", "");
    }

    public String getTheme(String jsonString) {
        return new JSONObject(jsonString).optString("theme", "");
    }

    public String getStatementId(String jsonString) {
        return new JSONObject(jsonString).optString("statementId", "");
    }

    public String getText(String jsonString) {
        return new JSONObject(jsonString).optString("text", "");
    }

    public String getCode(String jsonString) {
        return new JSONObject(jsonString).optString("code", "");
    }

    public Integer getTtlMinutes(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        return obj.has("ttlMinutes") && !obj.isNull("ttlMinutes") ? obj.getInt("ttlMinutes") : null;
    }

    public String getResetUrl(String jsonString) {
        return new JSONObject(jsonString).optString("resetUrl", "");
    }

    public String getGreetingName(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        String first = obj.optString("firstName", "").trim();
        String middle = obj.optString("middleName", "").trim();
        if (first.isEmpty() && middle.isEmpty()) return "";
        if (first.isEmpty()) return middle;
        if (middle.isEmpty()) return first;
        return first + " " + middle;
    }

    public String getFormattedAmount(String jsonString) {
        return formatMoney(optBigDecimal(jsonString, "amount"));
    }

    public Integer getTerm(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        return obj.has("term") && !obj.isNull("term") ? obj.getInt("term") : null;
    }

    public String getFormattedRate(String jsonString) {
        BigDecimal rate = optBigDecimal(jsonString, "rate");
        if (rate == null) return "";
        DecimalFormat df = new DecimalFormat("#,##0.##", RU_SYMBOLS);
        return df.format(rate);
    }

    public String getFormattedMonthlyPayment(String jsonString) {
        return formatMoney(optBigDecimal(jsonString, "monthlyPayment"));
    }

    private BigDecimal optBigDecimal(String jsonString, String key) {
        JSONObject obj = new JSONObject(jsonString);
        if (!obj.has(key) || obj.isNull(key)) return null;
        Object raw = obj.get(key);
        if (raw instanceof Number) {
            return new BigDecimal(raw.toString());
        }
        try {
            return new BigDecimal(raw.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) return "";
        DecimalFormat df = new DecimalFormat("#,##0.##", RU_SYMBOLS);
        return df.format(value) + " ₽";
    }

    private static DecimalFormatSymbols buildRuSymbols() {
        DecimalFormatSymbols s = new DecimalFormatSymbols(new Locale("ru", "RU"));
        s.setGroupingSeparator(' ');
        s.setDecimalSeparator(',');
        return s;
    }
}
