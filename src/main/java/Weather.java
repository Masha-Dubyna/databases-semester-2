import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.api.objects.Message;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.util.Scanner;

public class Weather {
    private static Statement stmt;
    private static ResultSet rs;

    public static String getWeather(Message message, String city, Model model, Connection con) throws IOException, SQLException {
        URL url = new URL("http://api.openweathermap.org/data/2.5/weather?q=" + city + "&units=metric&appid=6fff53a641b9b9a799cfd6b079f5cd4e");

        Scanner in = new Scanner((InputStream) url.getContent());
        String result = "";
        while (in.hasNext()) {
            result += in.nextLine();
        }

        JSONObject object = new JSONObject(result);
        model.setName(object.getString("name"));

        JSONObject main = object.getJSONObject("main");
        model.setTemp(main.getDouble("temp"));
        model.setHumidity(main.getDouble("humidity"));
        model.setFeelsLike(main.getDouble("feels_like"));

        JSONArray getArray = object.getJSONArray("weather");
        for (int i = 0; i < getArray.length(); i++) {
            JSONObject obj = getArray.getJSONObject(i);
            model.setDescription((String) obj.get("description"));
        }
        writeToDB(message.getChatId().toString(), city, model, con);
        return "City: " + model.getName() + "\n" +
                "Temperature: " + model.getTemp() + "C" + "\n" +
                "This temperature feels like: " + model.getFeelsLike() + "C" + "\n" +
                "Humidity:" + model.getHumidity() + "%" + "\n" +
                "Description: " + model.getDescription();
    }

    public static void writeToDB(String chatId, String city, Model model, Connection con) throws SQLException {
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO information (chat_id, city, description, temperature, feelings, humidity) VALUES (?, ?, ?, ?, ?, ?)");
        try {

            ps.setString(1, chatId);
            ps.setString(2, city);
            ps.setString(3, model.getDescription());
            ps.setDouble(4, model.getTemp());
            ps.setDouble(5, model.getFeelsLike());
            ps.setDouble(6, model.getHumidity());
            ps.executeUpdate();
        } finally {
            ps.close();
        }
    }
}
