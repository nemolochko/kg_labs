import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import javax.imageio.ImageIO;

public class Main extends JFrame {

    private BufferedImage original;
    private BufferedImage result;

    private JLabel originalLabel = new JLabel();
    private JLabel resultLabel = new JLabel();

    private HistogramPanel histOriginal = new HistogramPanel();
    private HistogramPanel histResult = new HistogramPanel();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    public Main() {
        super("Лабораторная №2 — Обработка изображений (Swing)");

        setLayout(new BorderLayout());

        JPanel top = new JPanel();
        JButton loadBtn = new JButton("Загрузить");
        JButton saveBtn = new JButton("Сохранить");
        JButton applyBtn = new JButton("Применить");
        JButton resetBtn = new JButton("Сбросить");

        String[] methods = {
                "Линейное контрастирование",
                "Эквализация RGB",
                "Эквализация яркости (HSV)"
        };
        JComboBox<String> methodBox = new JComboBox<>(methods);

        top.add(loadBtn);
        top.add(methodBox);
        top.add(applyBtn);
        top.add(resetBtn);
        top.add(saveBtn);

        add(top, BorderLayout.NORTH);

        // Панель с изображениями
        JPanel imagesPanel = new JPanel(new GridLayout(2, 2));
        originalLabel.setHorizontalAlignment(JLabel.CENTER);
        resultLabel.setHorizontalAlignment(JLabel.CENTER);

        imagesPanel.add(wrap("Оригинал", originalLabel));
        imagesPanel.add(wrap("Результат", resultLabel));
        imagesPanel.add(wrap("Гистограмма (оригинал)", histOriginal));
        imagesPanel.add(wrap("Гистограмма (результат)", histResult));

        add(imagesPanel, BorderLayout.CENTER);

        // Слушатели
        loadBtn.addActionListener(e -> loadImage());
        saveBtn.addActionListener(e -> saveImage());
        resetBtn.addActionListener(e -> reset());
        applyBtn.addActionListener((ActionEvent e) -> {
            if (original == null) return;

            String m = (String) methodBox.getSelectedItem();
            switch (m) {
                case "Линейное контрастирование":
                    result = linearContrast(original);
                    break;
                case "Эквализация RGB":
                    result = equalizeRGB(original);
                    break;
                case "Эквализация яркости (HSV)":
                    result = equalizeHSV(original);
                    break;
            }
            updateResult();
        });

        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private JPanel wrap(String title, JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(title));
        p.add(c);
        return p;
    }

    // -------------------- Загрузка / Сохранение --------------------

    private void loadImage() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                original = ImageIO.read(fc.getSelectedFile());
                result = null;
                originalLabel.setIcon(new ImageIcon(original));
                histOriginal.setImage(original);
                histResult.setImage(null);
                resultLabel.setIcon(null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void saveImage() {
        if (result == null) return;

        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(result, "png", fc.getSelectedFile());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void reset() {
        if (original == null) return;
        result = null;
        resultLabel.setIcon(null);
        histResult.setImage(null);
    }

    private void updateResult() {
        if (result != null) {
            resultLabel.setIcon(new ImageIcon(result));
            histResult.setImage(result);
        }
    }

    // -------------------- Обработка --------------------

    /** Линейное контрастирование */
    private BufferedImage linearContrast(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int min = 255, max = 0;

        // находим минимум и максимум яркости
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                int v = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                min = Math.min(min, v);
                max = Math.max(max, v);
            }
        }

        double scale = 255.0 / (max - min + 1);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                int r = (int) ((c.getRed() - min) * scale);
                int g = (int) ((c.getGreen() - min) * scale);
                int b = (int) ((c.getBlue() - min) * scale);

                r = clamp(r);
                g = clamp(g);
                b = clamp(b);

                out.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return out;
    }

    /** Эквализация по каналам RGB */
    private BufferedImage equalizeRGB(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int[][] hists = new int[3][256];

        // собираем гистограммы
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                hists[0][c.getRed()]++;
                hists[1][c.getGreen()]++;
                hists[2][c.getBlue()]++;
            }

        // строим СКФ (CDF)
        int[][] lut = new int[3][256];
        for (int ch = 0; ch < 3; ch++) {
            int sum = 0;
            for (int i = 0; i < 256; i++) {
                sum += hists[ch][i];
                lut[ch][i] = (int) ((sum * 255.0) / (w * h));
            }
        }

        // применяем
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                int r = lut[0][c.getRed()];
                int g = lut[1][c.getGreen()];
                int b = lut[2][c.getBlue()];
                out.setRGB(x, y, new Color(r, g, b).getRGB());
            }

        return out;
    }

    /** Эквализация только яркости в HSV */
    private BufferedImage equalizeHSV(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        int[] hist = new int[256];

        float[][][] hsv = new float[w][h][3];

        // переводим в HSV и собираем гистограмму яркости (Value)
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                Color c = new Color(img.getRGB(x, y));
                float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
                hsv[x][y] = hsb;
                int v = (int) (hsb[2] * 255);
                hist[v]++;
            }

        // CDF
        int sum = 0;
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            sum += hist[i];
            lut[i] = (int) ((sum * 255.0) / (w * h));
        }

        // применяем
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                float H = hsv[x][y][0];
                float S = hsv[x][y][1];
                int v = (int) (hsv[x][y][2] * 255);
                float V2 = lut[v] / 255f;
                int rgb = Color.HSBtoRGB(H, S, V2);
                out.setRGB(x, y, rgb);
            }

        return out;
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // -------------------- Класс гистограммы --------------------

    static class HistogramPanel extends JPanel {
        int[] histR = new int[256];
        int[] histG = new int[256];
        int[] histB = new int[256];
        boolean hasImage = false;

        public void setImage(BufferedImage img) {
            hasImage = (img != null);
            if (!hasImage) {
                repaint();
                return;
            }

            Arrays.fill(histR, 0);
            Arrays.fill(histG, 0);
            Arrays.fill(histB, 0);

            int w = img.getWidth();
            int h = img.getHeight();

            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    Color c = new Color(img.getRGB(x, y));
                    histR[c.getRed()]++;
                    histG[c.getGreen()]++;
                    histB[c.getBlue()]++;
                }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!hasImage) return;

            int W = getWidth();
            int H = getHeight();

            int max = 0;
            for (int i = 0; i < 256; i++)
                max = Math.max(max, Math.max(histR[i], Math.max(histG[i], histB[i])));

            for (int i = 0; i < 256; i++) {
                int x = i * W / 256;

                int r = (int) (histR[i] * (H - 5) / (float) max);
                int gVal = (int) (histG[i] * (H - 5) / (float) max);
                int b = (int) (histB[i] * (H - 5) / (float) max);

                g.setColor(Color.RED);
                g.drawLine(x, H, x, H - r);

                g.setColor(Color.GREEN);
                g.drawLine(x + 1, H, x + 1, H - gVal);

                g.setColor(Color.BLUE);
                g.drawLine(x + 2, H, x + 2, H - b);
            }
        }
    }
}
