package com.szekspir;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;

import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.definition.type.FactType;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ShakespeareApp {

    private KieContainer kContainer;
    private KieSession kSession;
    private Map<String, Map<String, Object>> questions;
    private Map<String, Map<String, Object>> recommendations;
    
    private JFrame frame;
    private JLabel mainLabel; 
    private JPanel contentPanel; 

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ShakespeareApp().init();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void init() throws Exception {
        String[] languages = {"Polski", "English"};
        int choice = JOptionPane.showOptionDialog(null, 
                "Wybierz jêzyk / Choose language", "Start",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, languages, languages[0]);

        String jsonFile = (choice == 1) ? "data_en.json" : "data_pl.json";

        loadResources(jsonFile);

        KieServices ks = KieServices.Factory.get();
        kContainer = ks.getKieClasspathContainer();

        buildGui();

        startNewSession();
    }

    private void loadResources(String filename) {
        try {
            Gson gson = new Gson();
            Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(filename), 
                StandardCharsets.UTF_8
            );
            Map<String, Object> root = gson.fromJson(reader, 
                new TypeToken<Map<String, Object>>(){}.getType());
            
            questions = (Map<String, Map<String, Object>>) root.get("questions");
            recommendations = (Map<String, Map<String, Object>>) root.get("recommendations");
            
            System.out.println("Za³adowano zasoby: " + filename);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "B³¹d ³adowania pliku: " + filename);
            System.exit(1);
        }
    }

    private void startNewSession() {
        if (kSession != null) {
            kSession.dispose();
        }
        
        kSession = kContainer.newKieSession("ksession-rules");
        System.out.println("Nowa sesja uruchomiona.");

        kSession.fireAllRules();
        updateInterface();
    }

    private void buildGui() {
        frame = new JFrame("Shakespeare Expert");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout(10, 10));

        mainLabel = new JLabel("...", SwingConstants.CENTER);
        mainLabel.setFont(new Font("Serif", Font.BOLD, 24));
        mainLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        frame.add(mainLabel, BorderLayout.NORTH);

        contentPanel = new JPanel();
        frame.add(contentPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void updateInterface() {
        Collection<? extends Object> facts = kSession.getObjects();
        boolean somethingFound = false;

        for (Object fact : facts) {
            String className = fact.getClass().getSimpleName();

            if (className.equals("UiQuestion")) {
                renderQuestion(fact);
                somethingFound = true;
                break;
            } else if (className.equals("Recommendation")) {
                renderRecommendation(fact);
                somethingFound = true;
                break;
            }
        }
        
        if (!somethingFound) {
            mainLabel.setText("B³¹d: Silnik nie zwróci³ ¿adnych danych.");
        }
        
        frame.revalidate();
        frame.repaint();
    }

    private void renderQuestion(Object drlQuestion) {
        try {
            contentPanel.removeAll();
            contentPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            
            Method getId = drlQuestion.getClass().getMethod("getId");
            Method getAnswers = drlQuestion.getClass().getMethod("getAnswers");

            String qId = (String) getId.invoke(drlQuestion);
            List<String> answers = (List<String>) getAnswers.invoke(drlQuestion);

            Map<String, Object> qData = questions.get(qId);
            String questionText = (String) qData.get("text");
            Map<String, String> options = (Map<String, String>) qData.get("options");
            
            mainLabel.setText("<html><div style='text-align: center;'>" + questionText + "</div></html>");

            JPanel btnPanel = new JPanel(new GridLayout(0, 1, 10, 10));
            
            for (String ansId : answers) {
                String text = options.get(ansId);
                if (text == null) text = ansId;

                JButton btn = new JButton(text);
                btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
                btn.setPreferredSize(new Dimension(300, 40));
                btn.addActionListener(e -> submitAnswer(qId, ansId));
                btnPanel.add(btn);
            }
            contentPanel.add(btnPanel);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void renderRecommendation(Object drlRec) {
        try {
            contentPanel.removeAll();
            contentPanel.setLayout(new BorderLayout());

            Method getRecId = drlRec.getClass().getMethod("getRecId");
            String recId = (String) getRecId.invoke(drlRec);

            Map<String, Object> recData = recommendations.get(recId);
            if (recData == null) {
                mainLabel.setText("Brak danych dla ID: " + recId);
                return;
            }

            String title = (String) recData.get("title");
            String description = (String) recData.get("description");
            String imagePath = (String) recData.get("image");

            mainLabel.setText(title);

            JPanel centerPanel = new JPanel();
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

            try {
                if (imagePath != null && !imagePath.isEmpty()) {

                    String path = "images/" + imagePath;
                    java.net.URL imgUrl = getClass().getClassLoader().getResource(path);
                    
                    if (imgUrl != null) {
                        BufferedImage img = ImageIO.read(imgUrl);
                        Image scaledImg = img.getScaledInstance(350, -1, Image.SCALE_SMOOTH);
                        JLabel imgLabel = new JLabel(new ImageIcon(scaledImg));
                        imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        centerPanel.add(imgLabel);
                    } else {
                        System.err.println("Nie znaleziono obrazka: " + path);
                    }
                }
            } catch (Exception ex) {
                System.err.println("B³¹d ³adowania obrazka: " + ex.getMessage());
            }


            centerPanel.add(Box.createVerticalStrut(20));
            JTextArea descArea = new JTextArea(description);
            descArea.setWrapStyleWord(true);
            descArea.setLineWrap(true);
            descArea.setEditable(false);
            descArea.setOpaque(false);
            descArea.setFont(new Font("Serif", Font.ITALIC, 18));
            descArea.setAlignmentX(Component.CENTER_ALIGNMENT);
            descArea.setMaximumSize(new Dimension(500, 100));
            centerPanel.add(descArea);

            contentPanel.add(centerPanel, BorderLayout.CENTER);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
            
            JButton restartBtn = new JButton("Zacznij od nowa / Start Over");
            restartBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
            restartBtn.addActionListener(e -> startNewSession());
            bottomPanel.add(restartBtn);
            
            JButton alternativeBtn = new JButton("Rekomendacja mi nie odpowiada / Not for me");
            alternativeBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
            alternativeBtn.addActionListener(e -> requestAlternative());
            bottomPanel.add(alternativeBtn);
            
            contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void submitAnswer(String qId, String ansId) {
        try {
            FactType type = kSession.getKieBase().getFactType("com.szekspir.rules", "UserAnswer");
            Object ans = type.newInstance();
            type.set(ans, "questionId", qId);
            type.set(ans, "selectedOption", ansId);

            kSession.insert(ans);
            kSession.fireAllRules();

            if (processUiCommands()) return;
            updateInterface();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean processUiCommands() {
        try {
            Collection<? extends Object> facts = kSession.getObjects();
            boolean restart = false;

            for (Object fact : facts) {
                if ("UiCommand".equals(fact.getClass().getSimpleName())) {
                    try {
                        Method getName = fact.getClass().getMethod("getName");
                        String name = (String) getName.invoke(fact);
                        if ("RESTART_SESSION".equals(name)) {
                            restart = true;
                        }
                    } catch (Exception ignored) {}
                }
            }

            if (restart) {
                startNewSession();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void requestAlternative() {
        try {
            String currentRecId = null;
            for (Object fact : kSession.getObjects()) {
                if ("Recommendation".equals(fact.getClass().getSimpleName())) {
                    try {
                        Method getRecId = fact.getClass().getMethod("getRecId");
                        currentRecId = (String) getRecId.invoke(fact);
                        break;
                    } catch (Exception ignored) {}
                }
            }

            FactType type = kSession.getKieBase().getFactType("com.szekspir.rules", "UserRejected");
            Object rejected = type.newInstance();

            try { type.set(rejected, "recId", currentRecId); } catch (Exception ignored) {}

            kSession.insert(rejected);
            kSession.fireAllRules();

            if (processUiCommands()) return;
            updateInterface();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
