import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;

public class App extends JFrame {
    private boolean isUpdating = false;

    private JSlider sliderRed, sliderGreen, sliderBlue;
    private JSlider sliderCyan, sliderMagenta, sliderYellow, sliderBlack;
    private JSlider sliderHue, sliderSaturation, sliderLightness;

    private JTextField fieldRed, fieldGreen, fieldBlue;
    private JTextField fieldCyan, fieldMagenta, fieldYellow, fieldBlack;
    private JTextField fieldHue, fieldSaturation, fieldLightness;

    private JPanel colorPreview;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::new);
    }

    public App() {
        setTitle("CMYK-RGB-HSL");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        sliderRed = createSlider(0, 255);
        sliderGreen = createSlider(0, 255);
        sliderBlue = createSlider(0, 255);

        sliderCyan = createSlider(0, 100);
        sliderMagenta = createSlider(0, 100);
        sliderYellow = createSlider(0, 100);
        sliderBlack = createSlider(0, 100);

        sliderHue = createSlider(0, 360);
        sliderSaturation = createSlider(0, 100);
        sliderLightness = createSlider(0, 100);

        fieldRed = createTextField();
        fieldGreen = createTextField();
        fieldBlue = createTextField();

        fieldCyan = createTextField();
        fieldMagenta = createTextField();
        fieldYellow = createTextField();
        fieldBlack = createTextField();

        fieldHue = createTextField();
        fieldSaturation = createTextField();
        fieldLightness = createTextField();

        JPanel panelRGB = createRGBHSLPanel("RGB", sliderRed, sliderGreen, sliderBlue, fieldRed, fieldGreen, fieldBlue);
        JPanel panelCMYK = createCMYKPanel("CMYK", sliderCyan, sliderMagenta, sliderYellow, sliderBlack, fieldCyan, fieldMagenta, fieldYellow, fieldBlack);
        JPanel panelHSL = createRGBHSLPanel("HSL", sliderHue, sliderSaturation, sliderLightness, fieldHue, fieldSaturation, fieldLightness);

        JPanel topPanel = new JPanel(new GridLayout(1, 3, 5, 5));
        topPanel.add(panelRGB);
        topPanel.add(panelCMYK);
        topPanel.add(panelHSL);
        add(topPanel, BorderLayout.NORTH);

        colorPreview = new JPanel();
        colorPreview.setPreferredSize(new Dimension(300, 300));
        colorPreview.setBackground(Color.WHITE);
        add(colorPreview, BorderLayout.CENTER);

        setSize(600, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        ChangeListener rgbListener = e -> {
            if (isUpdating) return;
            isUpdating = true;
            int r = sliderRed.getValue(), g = sliderGreen.getValue(), b = sliderBlue.getValue();
            updateFromRGB(r, g, b);
            isUpdating = false;
        };
        sliderRed.addChangeListener(rgbListener);
        sliderGreen.addChangeListener(rgbListener);
        sliderBlue.addChangeListener(rgbListener);

        ChangeListener cmykListener = e -> {
            if (isUpdating) return;
            isUpdating = true;
            double c = sliderCyan.getValue() / 100.0;
            double m = sliderMagenta.getValue() / 100.0;
            double y = sliderYellow.getValue() / 100.0;
            double k = sliderBlack.getValue() / 100.0;
            int[] rgb = cmykToRgb(c, m, y, k);
            sliderRed.setValue(rgb[0]);
            sliderGreen.setValue(rgb[1]);
            sliderBlue.setValue(rgb[2]);
            updateFromRGB(rgb[0], rgb[1], rgb[2]);
            isUpdating = false;
        };
        sliderCyan.addChangeListener(cmykListener);
        sliderMagenta.addChangeListener(cmykListener);
        sliderYellow.addChangeListener(cmykListener);
        sliderBlack.addChangeListener(cmykListener);

        ChangeListener hslListener = e -> {
            if (isUpdating) return;
            isUpdating = true;
            double h = sliderHue.getValue();
            double s = sliderSaturation.getValue();
            double l = sliderLightness.getValue();
            int[] rgb = hslToRgb(h, s, l);
            sliderRed.setValue(rgb[0]);
            sliderGreen.setValue(rgb[1]);
            sliderBlue.setValue(rgb[2]);
            updateFromRGB(rgb[0], rgb[1], rgb[2]);
            isUpdating = false;
        };
        sliderHue.addChangeListener(hslListener);
        sliderSaturation.addChangeListener(hslListener);
        sliderLightness.addChangeListener(hslListener);
    }

    private JPanel createRGBHSLPanel(String title, JSlider s1, JSlider s2, JSlider s3, JTextField t1, JTextField t2, JTextField t3) {
        JPanel panel = new JPanel(new GridLayout(3, 3));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        String[] labels = title.equals("RGB") ? new String[]{"R", "G", "B"} : new String[]{"H", "S", "L"};
        JSlider[] sliders = {s1, s2, s3};
        JTextField[] fields = {t1, t2, t3};
        for (int i = 0; i < 3; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(sliders[i]);
            panel.add(fields[i]);
            bindSliderToField(sliders[i], fields[i]);
        }
        return panel;
    }

    private JPanel createCMYKPanel(String title, JSlider s1, JSlider s2, JSlider s3, JSlider s4, JTextField t1, JTextField t2, JTextField t3, JTextField t4) {
        JPanel panel = new JPanel(new GridLayout(4, 3));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        String[] labels = {"C", "M", "Y", "K"};
        JSlider[] sliders = {s1, s2, s3, s4};
        JTextField[] fields = {t1, t2, t3, t4};
        for (int i = 0; i < 4; i++) {
            panel.add(new JLabel(labels[i]));
            panel.add(sliders[i]);
            panel.add(fields[i]);
            bindSliderToField(sliders[i], fields[i]);
        }
        return panel;
    }

    private JSlider createSlider(int min, int max) {
        JSlider slider = new JSlider(min, max, min);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }

    private JTextField createTextField() {
        return new JTextField(4);
    }

    private void bindSliderToField(JSlider slider, JTextField field) {
        slider.addChangeListener(e -> field.setText(Integer.toString(slider.getValue())));
        field.addActionListener(e -> {
            try {
                slider.setValue(Integer.parseInt(field.getText()));
            } catch (Exception ignored) {}
        });
    }

    private void updateFromRGB(int r, int g, int b) {
        double[] cmyk = rgbToCmyk(r, g, b);
        sliderCyan.setValue((int)Math.round(cmyk[0]*100));
        sliderMagenta.setValue((int)Math.round(cmyk[1]*100));
        sliderYellow.setValue((int)Math.round(cmyk[2]*100));
        sliderBlack.setValue((int)Math.round(cmyk[3]*100));

        double[] hsl = rgbToHsl(r, g, b);
        sliderHue.setValue((int)Math.round(hsl[0]));
        sliderSaturation.setValue((int)Math.round(hsl[1]));
        sliderLightness.setValue((int)Math.round(hsl[2]));

        colorPreview.setBackground(new Color(r, g, b));
    }

    private static double[] rgbToCmyk(int r, int g, int b) {
        double rd = r / 255.0, gd = g / 255.0, bd = b / 255.0;
        double k = 1 - Math.max(rd, Math.max(gd, bd));
        double c = (1 - rd - k) / (1 - k + 1e-10);
        double m = (1 - gd - k) / (1 - k + 1e-10);
        double y = (1 - bd - k) / (1 - k + 1e-10);
        if (Math.abs(k - 1) < 1e-10) c = m = y = 0;
        return new double[]{c, m, y, k};
    }

    private static int[] cmykToRgb(double c, double m, double y, double k) {
        int r = (int)Math.round(255 * (1 - c) * (1 - k));
        int g = (int)Math.round(255 * (1 - m) * (1 - k));
        int b = (int)Math.round(255 * (1 - y) * (1 - k));
        return new int[]{r, g, b};
    }

    private static double[] rgbToHsl(int r, int g, int b) {
        double rd = r / 255.0, gd = g / 255.0, bd = b / 255.0;
        double max = Math.max(rd, Math.max(gd, bd));
        double min = Math.min(rd, Math.min(gd, bd));
        double h = 0, s, l = (max + min) / 2.0;
        if (max == min) {
            h = s = 0;
        } else {
            double d = max - min;
            s = d / (1 - Math.abs(2 * l - 1));
            if (max == rd) h = ((gd - bd) / d) % 6;
            else if (max == gd) h = ((bd - rd) / d) + 2;
            else h = ((rd - gd) / d) + 4;
            h *= 60;
            if (h < 0) h += 360;
        }
        return new double[]{h, s * 100, l * 100};
    }

    private static int[] hslToRgb(double h, double s, double l) {
        s /= 100;
        l /= 100;
        double c = (1 - Math.abs(2 * l - 1)) * s;
        double x = c * (1 - Math.abs((h / 60) % 2 - 1));
        double m = l - c / 2;
        double r1 = 0, g1 = 0, b1 = 0;
        if (h < 60) { r1 = c; g1 = x; }
        else if (h < 120) { r1 = x; g1 = c; }
        else if (h < 180) { g1 = c; b1 = x; }
        else if (h < 240) { g1 = x; b1 = c; }
        else if (h < 300) { r1 = x; b1 = c; }
        else { r1 = c; b1 = x; }
        int r = (int)Math.round((r1 + m) * 255);
        int g = (int)Math.round((g1 + m) * 255);
        int b = (int)Math.round((b1 + m) * 255);
        return new int[]{r, g, b};
    }
}
