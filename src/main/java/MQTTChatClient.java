import org.eclipse.paho.client.mqttv3.*;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

public class MQTTChatClient {
    private MqttClient client;
    private String username;
    private String baseUrl;
    private String chatPath;

    public MQTTChatClient(String serverUrl, String baseURL, String username, String chatPath) {
        this.username = username;
        this.baseUrl = baseURL;
        this.chatPath = chatPath;

        // Generar un identificador único para el cliente MQTT
        String publisherId = UUID.randomUUID().toString();

        try {
            this.client = new MqttClient(serverUrl, publisherId);
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    public void connect() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(5);

        // Conectar al servidor MQTT y establecer callbacks
        client.connect(options);
        setCallbacks();
    }

    public void close() throws MqttException {
        // Desconectar y cerrar el cliente MQTT
        client.disconnect();
        client.close();
    }

    public void sendMessage(String recipient, String message) throws MqttException {
        if (client.isConnected()) {
            String topic = getSendChatTopic(recipient);
            byte[] payload = message.getBytes();
            MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(0);
            mqttMessage.setRetained(true);

            // Publicar el mensaje en el tema
            client.publish(topic, mqttMessage);
        }
    }

    public String getChat(String otherUser) throws IOException {
        Path filename = Path.of(chatPath, "chat" + otherUser);

        // Leer el historial de chat desde el archivo
        try (BufferedReader br = Files.newBufferedReader(filename)) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private void setCallbacks() throws MqttException {
        // Configurar callbacks para eventos MQTT
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                // Manejar pérdida de conexión
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                writeReceivedMessage(topic, mqttMessage);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                try {
                    String topic = iMqttDeliveryToken.getTopics()[0];
                    MqttMessage mqttMessage = iMqttDeliveryToken.getMessage();
                    writeSentMessage(topic, mqttMessage);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Suscribirse a temas específicos
        String[] topics = new String[]{String.format("/%s/todos", baseUrl), String.format("/%s/+/%s", baseUrl, username)};
        client.subscribe(topics, new int[]{0, 0});
    }

    private String getSendChatTopic(String recipient) {
        // Construir el tema MQTT para enviar mensajes a un usuario específico
        if (recipient.equals("todos")) {
            return String.format("/%s/%s", baseUrl, recipient);
        } else {
            return String.format("/%s/%s/%s", baseUrl, username, recipient);
        }
    }

    private void writeReceivedMessage(String topic, MqttMessage mqttMessage) throws IOException {
        String[] topicSplit = topic.split("/");
        Path filename = Path.of(chatPath, topicSplit[1] + topicSplit[2]);

        // Escribir el mensaje recibido en el archivo
        try (BufferedWriter bw = Files.newBufferedWriter(filename, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            bw.write("%s (%s): %s".formatted(topicSplit[2], LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), mqttMessage.toString()));
            bw.newLine();
        }
    }

    private void writeSentMessage(String topic, MqttMessage mqttMessage) throws IOException {
        String[] topicSplit = topic.split("/");
        if (!topicSplit[2].equals("todos")) {
            // El chat todos se recibe por subscribe, no es necesario escribirlo
            Path filename = Path.of(chatPath, topicSplit[1] + topicSplit[3]);

            // Escribir el mensaje enviado en el archivo
            try (BufferedWriter bw = Files.newBufferedWriter(filename, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                bw.write("%s (%s): %s".formatted(username, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), mqttMessage.toString()));
                bw.newLine();
            }
        }
    }
}
