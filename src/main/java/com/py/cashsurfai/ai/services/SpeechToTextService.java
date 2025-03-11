package com.py.cashsurfai.ai.services;

//import com.google.cloud.speech.v1.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SpeechToTextService {

    private static final String PYTHON_PATH = "C:\\whisper_env\\Scripts\\python.exe"; // O especifica la ruta completa si está en el entorno virtual
    //    private static final String WHISPER_SCRIPT = "import whisper; model = whisper.load_model('small'); result = model.transcribe('%s', language='es'); print(result['text'])";
    //    private static final String WHISPER_SCRIPT = "import whisper; import torch; import warnings; warnings.filterwarnings('ignore', category=FutureWarning); model = whisper.load_model('small').to('cuda' if torch.cuda.is_available() else 'cpu'); result = model.transcribe('%s', language='es'); print(result['text'])";
    private static final String SCRIPT_PATH = "C:\\whisper_env\\scripts-av\\extract_expensas.py";

    public String convertAudioToText(byte[] audioBytes) throws IOException {
        Path tempFile = Files.createTempFile("audio_", ".wav");
        Files.write(tempFile, audioBytes);
        log.info("Temp audio path: {}", tempFile);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    PYTHON_PATH, SCRIPT_PATH, tempFile.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean exited = process.waitFor(60, TimeUnit.SECONDS);
            if (exited) {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new IOException("Whisper failed with exit code: " + exitCode + ". Output: " + output);
                }
            } else {
                process.destroy();
                throw new IOException("Whisper process timed out after 30 seconds. Output: " + output);
            }

            String fullOutput = output.toString().trim();
            log.debug("Full Whisper output: {}", fullOutput);
            return fullOutput; // No necesitas extractAudioText si el script imprime solo la transcripción

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running Whisper", e);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

//    public String convertAudioToText(byte[] audioBytes) throws IOException {
//        // Usar una ruta con separadores Windows y escapada correctamente
//        Path tempFile = Files.createTempFile("audio_"+ System.currentTimeMillis(), ".wav");
//        String tempAudioPath = tempFile.toAbsolutePath().toString();
//        Files.write(tempFile, audioBytes);
//        log.info("Temp audio path: {}", tempFile);
//        // Depurar la ruta generada
//        try {
//            // Escapar la ruta para Python usando raw string (r'...')
//            String escapedPath = tempAudioPath.replace("\\", "\\\\");
//            ProcessBuilder pb = new ProcessBuilder(
//                    PYTHON_PATH, "-c",
//                    String.format(WHISPER_SCRIPT, escapedPath)
//            );
//            pb.redirectErrorStream(true); // Combinar stdout y stderr como antes
//            Process process = pb.start();
//
//            StringBuilder output = new StringBuilder();
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    output.append(line).append("\n");
//                }
//            }
//
//            // Esperar con timeout
//            boolean exited = process.waitFor(30, TimeUnit.SECONDS);
//            if (exited) {
//                int exitCode = process.exitValue(); // Obtener el código de salida solo si terminó
//                if (exitCode != 0) {
//                    throw new IOException("Whisper failed with exit code: " + exitCode + ". Output: " + output);
//                }
//            } else {
//                // Si no terminó a tiempo, forzar la terminación
//                process.destroy();
//                throw new IOException("Whisper process timed out after 30 seconds. Output: " + output);
//            }
//
//            // Filtrar el texto del audio con regex
//            String fullOutput = output.toString().trim();
//            log.debug("Full Whisper output: {}", fullOutput);
//
//            return extractAudioText(fullOutput);
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new IOException("Interrupted while running Whisper", e);
//        } finally {
//            Files.deleteIfExists(Paths.get(tempAudioPath));
//        }
//    }

    private String extractAudioText(String rawTranscript) {
        String marker = "checkpoint = torch.load(fp, map_location=device)";
        int index = rawTranscript.indexOf(marker);
        return (index == -1) ? rawTranscript.trim() : rawTranscript.substring(index + marker.length()).trim();
    }

    public static void testPythonPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, "--version");
            Process p = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            System.out.println(reader.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Google Service
//    public String convertAudioToText(byte[] audioBytes) throws IOException {
//        try (SpeechClient speechClient = SpeechClient.create()) {
//            RecognitionConfig config = RecognitionConfig.newBuilder()
//                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16) // Ajusta según formato de audio
//                    .setSampleRateHertz(16000) // Ajusta según tu audio
//                    .setLanguageCode("es-ES") // Español, ajusta según necesidad
//                    .build();
//
//            RecognitionAudio audio = RecognitionAudio.newBuilder()
//                    .setContent(com.google.protobuf.ByteString.copyFrom(audioBytes))
//                    .build();
//
//            RecognizeResponse response = speechClient.recognize(config, audio);
//            if (response.getResultsList().isEmpty()) {
//                return null;
//            }
//            return response.getResultsList().get(0).getAlternativesList().get(0).getTranscript();
//        }
//    }
}