package com.example.calculatrice;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView display, modeLabel;
    private GridLayout keypad;
    private SharedPreferences prefs;
    private String expression = "";
    private double memory = 0;
    private boolean scientific = false;
    private boolean degrees = true;
    private boolean haptics = true;
    private int themeIndex = 0;
    private final ArrayList<String> history = new ArrayList<>();

    private int bg, panel, fg, accent, operatorColor;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("calculator", MODE_PRIVATE);
        memory = Double.longBitsToDouble(
            prefs.getLong("memory", Double.doubleToLongBits(0)));
        scientific = prefs.getBoolean("scientific", false);
        degrees = prefs.getBoolean("degrees", true);
        haptics = prefs.getBoolean("haptics", true);
        themeIndex = prefs.getInt("theme", 0);

        String savedHistory = prefs.getString("history", "");
        for (String item : savedHistory.split("\n")) {
            if (!item.trim().isEmpty()) history.add(item);
        }

        buildUI();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void saveSettings() {
        StringBuilder items = new StringBuilder();
        int start = Math.max(0, history.size() - 40);
        for (int i = start; i < history.size(); i++) {
            items.append(history.get(i).replace("\n", " "))
                 .append('\n');
        }
        prefs.edit()
            .putLong("memory", Double.doubleToLongBits(memory))
            .putBoolean("scientific", scientific)
            .putBoolean("degrees", degrees)
            .putBoolean("haptics", haptics)
            .putInt("theme", themeIndex)
            .putString("history", items.toString())
            .apply();
    }

    private void applyColors() {
        if (themeIndex == 1) {
            bg = Color.rgb(244, 246, 250);
            panel = Color.WHITE;
            fg = Color.rgb(30, 36, 48);
            accent = Color.rgb(36, 105, 220);
            operatorColor = Color.rgb(220, 230, 246);
        } else if (themeIndex == 2) {
            bg = Color.rgb(12, 28, 42);
            panel = Color.rgb(22, 46, 64);
            fg = Color.rgb(225, 244, 255);
            accent = Color.rgb(0, 177, 170);
            operatorColor = Color.rgb(34, 68, 86);
        } else {
            bg = Color.rgb(17, 19, 26);
            panel = Color.rgb(30, 33, 43);
            fg = Color.rgb(244, 245, 249);
            accent = Color.rgb(255, 166, 67);
            operatorColor = Color.rgb(48, 52, 66);
        }
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
    }

    private TextView text(String value, float size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(fg);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(fg);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(color));
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setOnClickListener(v -> {
            if (haptics) {
                v.performHapticFeedback(
                    HapticFeedbackConstants.KEYBOARD_TAP);
            }
            handle(label);
        });
        return b;
    }

    private void buildUI() {
        applyColors();

        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(8), dp(12), dp(12));
        root.setBackgroundColor(bg);
        scroll.setFillViewport(true);
        scroll.addView(root);
        setContentView(scroll);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER);
        root.addView(toolbar);

        addTool(toolbar, scientific ? "عادي" : "علمي",
            () -> { scientific = !scientific; saveSettings(); buildUI(); });
        addTool(toolbar, degrees ? "DEG" : "RAD",
            () -> { degrees = !degrees; saveSettings(); buildUI(); });
        addTool(toolbar, "المظهر",
            () -> { themeIndex = (themeIndex + 1) % 3; saveSettings(); buildUI(); });
        addTool(toolbar, haptics ? "اهتزاز ✓" : "اهتزاز ✕",
            () -> { haptics = !haptics; saveSettings(); buildUI(); });
        addTool(toolbar, "السجل",
            this::showHistory);

        modeLabel = text(scientific ? "SCIENTIFIC" : "STANDARD", 12);
        modeLabel.setTextColor(accent);
        modeLabel.setPadding(dp(8), dp(8), dp(8), 0);
        root.addView(modeLabel);

        display = text(expression.isEmpty() ? "0" : expression, 30);
        display.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        display.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        display.setPadding(dp(12), dp(12), dp(12), dp(12));
        display.setMinHeight(dp(104));
        display.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(panel));
        display.setBackgroundColor(panel);
        root.addView(display);

        TextView memoryLabel = text(
            "الذاكرة: " + format(memory), 13);
        memoryLabel.setTextColor(accent);
        memoryLabel.setPadding(dp(6), dp(8), dp(6), dp(4));
        root.addView(memoryLabel);

        LinearLayout memoryRow = new LinearLayout(this);
        root.addView(memoryRow);
        addTool(memoryRow, "MC", () -> { memory = 0; saveSettings(); buildUI(); });
        addTool(memoryRow, "MR", () -> append(format(memory)));
        addTool(memoryRow, "M+", () -> { memory += currentValue(); saveSettings(); buildUI(); });
        addTool(memoryRow, "M−", () -> { memory -= currentValue(); saveSettings(); buildUI(); });

        keypad = new GridLayout(this);
        keypad.setColumnCount(scientific ? 5 : 4);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.topMargin = dp(8);
        root.addView(keypad, gridParams);

        String[] normal = {
            "C", "DEL", "(", ")", "7", "8", "9", "÷",
            "4", "5", "6", "×", "1", "2", "3", "−",
            "±", "0", ".", "+", "%", "=", "x²", "√"
        };
        String[] sci = {
            "sin", "cos", "tan", "log", "ln",
            "√", "x²", "xʸ", "π", "e",
            "(", ")", "7", "8", "9",
            "C", "DEL", "4", "5", "6",
            "÷", "×", "1", "2", "3",
            "−", "±", "0", ".", "+",
            "%", "=", "sin⁻¹", "cos⁻¹", "tan⁻¹"
        };
        String[] keys = scientific ? sci : normal;

        for (String key : keys) {
            int color = key.equals("=") ? accent :
                ("÷×−+^".contains(key) || key.equals("x²") ||
                 key.equals("xʸ") || key.equals("√"))
                    ? operatorColor : panel;

            Button b = button(key, color);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0;
            p.height = dp(52);
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            p.setMargins(dp(3), dp(3), dp(3), dp(3));
            keypad.addView(b, p);
        }
    }

    private void addTool(LinearLayout row, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(11);
        b.setTextColor(fg);
        b.setMinWidth(0);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(operatorColor));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            0, dp(44), 1);
        p.setMargins(dp(2), dp(2), dp(2), dp(2));
        row.addView(b, p);
        b.setOnClickListener(v -> {
            if (haptics) v.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP);
            action.run();
        });
    }

    private void append(String value) {
        expression += value;
        refreshDisplay();
    }

    private void refreshDisplay() {
        display.setText(expression.isEmpty() ? "0" : expression);
    }

    private double currentValue() {
        if (expression.trim().isEmpty()) return 0;
        try {
            return new Parser(expression, degrees).parse();
        } catch (Exception e) {
            return 0;
        }
    }

    private void handle(String key) {
        switch (key) {
            case "C":
                expression = "";
                refreshDisplay();
                return;
            case "DEL":
                if (!expression.isEmpty()) {
                    expression = expression.substring(0, expression.length() - 1);
                }
                refreshDisplay();
                return;
            case "=":
                calculate();
                return;
            case "±":
                expression = expression.isEmpty() ? "-" : "-(" + expression + ")";
                refreshDisplay();
                return;
            case "x²": append("^2"); return;
            case "xʸ": append("^"); return;
            case "√": append("sqrt("); return;
            case "π": append("pi"); return;
            case "e": append("e"); return;
            case "sin": case "cos": case "tan":
            case "log": case "ln":
                append(key + "("); return;
            case "sin⁻¹": append("asin("); return;
            case "cos⁻¹": append("acos("); return;
            case "tan⁻¹": append("atan("); return;
            case "÷": append("/"); return;
            case "×": append("*"); return;
            case "−": append("-"); return;
            default:
                append(key);
        }
    }

    private void calculate() {
        if (expression.trim().isEmpty()) return;
        String original = expression;
        try {
            double result = new Parser(expression, degrees).parse();
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                throw new ArithmeticException("نتيجة غير صالحة");
            }
            String output = format(result);
            history.add(original + " = " + output);
            expression = output;
            saveSettings();
            refreshDisplay();
        } catch (Exception e) {
            Toast.makeText(this, "تحقق من العملية الحسابية",
                Toast.LENGTH_SHORT).show();
        }
    }

    private String format(double value) {
        if (value == 0) return "0";
        if (Math.abs(value) < 1e15 &&
            Math.abs(value - Math.rint(value)) < 1e-10) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.10g", value);
    }

    private void showHistory() {
        if (history.isEmpty()) {
            Toast.makeText(this, "السجل فارغ", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            items[i] = history.get(history.size() - 1 - i);
        }
        new AlertDialog.Builder(this)
            .setTitle("سجل العمليات")
            .setItems(items, (dialog, which) -> {
                String item = items[which];
                int equals = item.lastIndexOf(" = ");
                if (equals >= 0) expression = item.substring(equals + 3);
                else expression = item;
                refreshDisplay();
            })
            .setNeutralButton("مسح السجل", (dialog, which) -> {
                history.clear();
                saveSettings();
            })
            .setNegativeButton("إغلاق", null)
            .show();
    }

    // محلل رياضي: الأقواس، الأولوية، الأسس، النسب المئوية والدوال العلمية.
    private static class Parser {
        private final String s;
        private final boolean degrees;
        private int pos = 0;

        Parser(String input, boolean useDegrees) {
            s = input.replace("×", "*").replace("÷", "/")
                     .replace("−", "-").replace("π", "pi");
            degrees = useDegrees;
        }

        double parse() {
            double value = expression();
            spaces();
            if (pos != s.length()) throw new IllegalArgumentException();
            return value;
        }

        void spaces() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        boolean eat(char c) {
            spaces();
            if (pos < s.length() && s.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        double expression() {
            double x = term();
            while (true) {
                if (eat('+')) x += term();
                else if (eat('-')) x -= term();
                else return x;
            }
        }

        double term() {
            double x = unary();
            while (true) {
                if (eat('*')) x *= unary();
                else if (eat('/')) {
                    double d = unary();
                    if (d == 0) throw new ArithmeticException();
                    x /= d;
                } else return x;
            }
        }

        double unary() {
            if (eat('+')) return unary();
            if (eat('-')) return -unary();
            return power();
        }

        double power() {
            double x = postfix();
            if (eat('^')) x = Math.pow(x, unary());
            return x;
        }

        double postfix() {
            double x = primary();
            while (eat('%')) x /= 100.0;
            return x;
        }

        double primary() {
            spaces();
            if (eat('(')) {
                double x = expression();
                if (!eat(')')) throw new IllegalArgumentException();
                return x;
            }

            spaces();
            if (pos < s.length() &&
                (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) {
                int start = pos;
                while (pos < s.length() &&
                    (Character.isDigit(s.charAt(pos)) ||
                     s.charAt(pos) == '.' || s.charAt(pos) == 'e' ||
                     s.charAt(pos) == 'E' ||
                     ((s.charAt(pos) == '+' || s.charAt(pos) == '-') &&
                      pos > start &&
                      (s.charAt(pos - 1) == 'e' || s.charAt(pos - 1) == 'E')))) {
                    pos++;
                }
                return Double.parseDouble(s.substring(start, pos));
            }

            if (pos < s.length() && Character.isLetter(s.charAt(pos))) {
                int start = pos;
                while (pos < s.length() &&
                       Character.isLetter(s.charAt(pos))) pos++;
                String name = s.substring(start, pos).toLowerCase(Locale.US);

                if (name.equals("pi")) return Math.PI;
                if (name.equals("e")) return Math.E;

                if (!eat('(')) throw new IllegalArgumentException();
                double x = expression();
                if (!eat(')')) throw new IllegalArgumentException();

                switch (name) {
                    case "sqrt":
                        if (x < 0) throw new ArithmeticException();
                        return Math.sqrt(x);
                    case "sin": return Math.sin(angle(x));
                    case "cos": return Math.cos(angle(x));
                    case "tan": return Math.tan(angle(x));
                    case "asin": return inverseAngle(Math.asin(x));
                    case "acos": return inverseAngle(Math.acos(x));
                    case "atan": return inverseAngle(Math.atan(x));
                    case "log":
                        if (x <= 0) throw new ArithmeticException();
                        return Math.log10(x);
                    case "ln":
                        if (x <= 0) throw new ArithmeticException();
                        return Math.log(x);
                    default: throw new IllegalArgumentException();
                }
            }
            throw new IllegalArgumentException();
        }

        double angle(double x) {
            return degrees ? Math.toRadians(x) : x;
        }

        double inverseAngle(double x) {
            return degrees ? Math.toDegrees(x) : x;
        }
    }
}
