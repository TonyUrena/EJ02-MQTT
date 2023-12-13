import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class MQTTChat {
    public static void main(String[] args) {
        MQTTChatClient chatClient = new MQTTChatClient("tcp://3.83.123.224:1883", "chat", "Tony", "");
        try {

            chatClient.connect();

            String command = "";
            Scanner scanner = new Scanner(System.in);

                System.out.print("Enter command: ");
                String rawCommand = scanner.nextLine() + " ";
                String[] commandSplit = rawCommand.split(" ", 3);

                command = commandSplit[0];

                if (command.equals("send")) {

                    chatClient.sendMessage(commandSplit[1], commandSplit[2]);
                    System.out.printf("Sending %s to %s\r\n", commandSplit[2], commandSplit[1]);

                } else if (command.equals("chat")) {
                    System.out.println(chatClient.getChat(commandSplit[1]));
                }

            chatClient.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}