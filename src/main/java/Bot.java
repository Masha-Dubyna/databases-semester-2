import org.glassfish.jersey.client.spi.Connector;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    private static final String url = "jdbc:mysql://localhost:3306/fordb?serverTimezone=Europe/Kiev";
    private static final String user = "root";
    private static final String password = "root";
    private static Connection con = null;
    private static Statement stmt;
    private static ResultSet rs;
    private String forHelp = "Hi, I am weather bot:)\n"+"Available commands:\n" +"/help - show this message\n" +
            "/setCity - set your city\n" + "/getTemperature - show information about city that you set\n" +
            "You can get information without setting city. Just write correct name of city";
    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);
        try {
            setButtons(sendMessage);

            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        try {

            con = DriverManager.getConnection(url, user, password);

            Model model = new Model();
            Message message = update.getMessage();
            if (message != null && message.hasText()) {
                String text = message.getText();
                if ("/help".equals(text)) {
                    sendMsg(message, forHelp);
                } else if ( text.length() > 8 && text.substring(0, 8).equals("/setCity")) {
                    String city = new String();
                    try {
                        city = text.substring(9, text.length());
                        writeCityToDB(message.getChatId().toString(), con, city);
                        sendMsg(message, Weather.getWeather(message, city, model, con));
                        sendMsg(message, "Now your city is " + city);
                    } catch (StringIndexOutOfBoundsException ex) {
                        sendMsg(message, "Write a correct city name!");
                    } catch (IOException e) {
                        sendMsg(message, "Write a correct city name!");
                        deleteWrongData(message.getChatId().toString(), con, city);
                    }
                } else if (text.equals("/getTemperature")) {
                    String city = getMyCity(message.getChatId().toString(), con);
                    sendMsg(message, Weather.getWeather(message, city, model, con));
                } else {
                    try {
                        sendMsg(message, Weather.getWeather(message, message.getText(), model, con));
                    } catch (IOException e) {
                        sendMsg(message, "City not found!");
                    }
                }
            }
        } catch (SQLException | IOException sqlEx) {
            sqlEx.printStackTrace();
        } finally {
            try { con.close(); } catch(SQLException se) { }
        }

    }

    public void setButtons(SendMessage sendMessage) {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRowList = new ArrayList<KeyboardRow>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add(new KeyboardButton("/help"));
        keyboardFirstRow.add(new KeyboardButton("/getTemperature"));

        keyboardRowList.add(keyboardFirstRow);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
    }

    public String getMyCity(String chatId, Connection con) throws SQLException  {
        PreparedStatement ps = con.prepareStatement(
                "SELECT city FROM my_city WHERE chat_id=?");
        String  res = new String();
        try {
            ps.setString(1, chatId);

            ResultSet rs = ps.executeQuery();
            try {
                ResultSetMetaData md = rs.getMetaData();


                while (rs.next()) {
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        res = rs.getString(i);
                    }
                    System.out.println();
                }
            } finally {
                rs.close();
            }

            //res = ps.getResultSet().toString();
        } finally {
            ps.close();
        }
        //System.out.println(res);
        return res;
    }

    public void writeCityToDB(String chatId, Connection con, String city) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO my_city (chat_id, city) VALUES (?, ?)");
        try {

            ps.setString(1, chatId);
            ps.setString(2, city);
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    public void deleteWrongData(String chatId, Connection con, String city) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "DELETE FROM my_city WHERE chat_id=? AND city=?");
        try {

            ps.setString(1, chatId);
            ps.setString(2, city);
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    public String getBotUsername() {
        return "MyLovelyNameBot";
    }

    public String getBotToken() {
        return null;
    }
}
