package com.fiap.esoa.salesmind.service;

import com.fiap.esoa.salesmind.enums.CategoriaAmbiental;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class GeminiService {

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    private static final int MAX_POLLING_ATTEMPTS = 60;

    public GeminiService() {
        this.apiKey = System.getProperty("GEMINI_API_KEY", System.getenv("GEMINI_API_KEY"));
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("AVISO: GEMINI_API_KEY não configurada. Recursos de IA não funcionarão.");
        }

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Faz upload de arquivo de áudio para a API de Arquivos do Gemini
     * 
     * @param audioFilePath Caminho para o arquivo de áudio local
     * @return URI do arquivo para uso em generateContent
     */
    private String uploadToGemini(String audioFilePath) throws IOException, InterruptedException {
        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            throw new IOException("Arquivo de áudio não encontrado: " + audioFilePath);
        }

        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());
        String mimeType = Files.probeContentType(Path.of(audioFilePath));
        if (mimeType == null) {
            String fileName = audioFile.getName().toLowerCase();
            if (fileName.endsWith(".mp3"))
                mimeType = "audio/mp3";
            else if (fileName.endsWith(".wav"))
                mimeType = "audio/wav";
            else if (fileName.endsWith(".m4a"))
                mimeType = "audio/mp4";
            else if (fileName.endsWith(".ogg"))
                mimeType = "audio/ogg";
            else if (fileName.endsWith(".flac"))
                mimeType = "audio/flac";
            else
                mimeType = "audio/mpeg";
        }

        String uploadUrl = initiateResumableUpload(audioFile.getName(), mimeType, audioBytes.length);

        String fileUri = uploadFileBytes(uploadUrl, audioBytes);

        return pollFileStatus(fileUri);
    }

    /**
     * Iniciar upload retomável e obter URL de upload
     */
    private String initiateResumableUpload(String displayName, String mimeType, long numBytes)
            throws IOException, InterruptedException {

        ObjectNode metadata = objectMapper.createObjectNode();
        ObjectNode file = objectMapper.createObjectNode();
        file.put("display_name", displayName);
        metadata.set("file", file);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_API_BASE + "/upload/v1beta/files?key=" + apiKey))
                .header("X-Goog-Upload-Protocol", "resumable")
                .header("X-Goog-Upload-Command", "start")
                .header("X-Goog-Upload-Header-Content-Length", String.valueOf(numBytes))
                .header("X-Goog-Upload-Header-Content-Type", mimeType)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(metadata.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Falha ao iniciar upload: " + response.statusCode() + " - " + response.body());
        }

        return response.headers().firstValue("X-Goog-Upload-URL")
                .orElseThrow(() -> new IOException("Nenhuma URL de upload na resposta"));
    }

    /**
     * Fazer upload dos bytes do arquivo para a URL de upload retomável
     */
    private String uploadFileBytes(String uploadUrl, byte[] fileBytes)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("X-Goog-Upload-Offset", "0")
                .header("X-Goog-Upload-Command", "upload, finalize")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Falha ao fazer upload do arquivo: " + response.statusCode() + " - " + response.body());
        }

        JsonNode responseJson = objectMapper.readTree(response.body());
        return responseJson.get("file").get("uri").asText();
    }

    /**
     * Fazer polling do status do arquivo até estar ATIVO
     */
    private String pollFileStatus(String fileUri) throws IOException, InterruptedException {
        String fileName = fileUri.substring(fileUri.lastIndexOf('/') + 1);
        String statusUrl = GEMINI_API_BASE + "/v1beta/files/" + fileName + "?key=" + apiKey;

        for (int attempt = 0; attempt < MAX_POLLING_ATTEMPTS; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(statusUrl))
                    .timeout(REQUEST_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode fileInfo = objectMapper.readTree(response.body());
                String state = fileInfo.get("state").asText();

                if ("ACTIVE".equals(state)) {
                    return fileUri;
                } else if ("FAILED".equals(state)) {
                    String error = fileInfo.has("error") ? fileInfo.get("error").toString() : "Erro desconhecido";
                    throw new IOException("Processamento do arquivo falhou: " + error);
                }
            }

            Thread.sleep(5000);
        }

        throw new IOException("Timeout no processamento do arquivo - arquivo não ficou ATIVO após " +
                (MAX_POLLING_ATTEMPTS * 5) + " segundos");
    }

    /**
     * Transcrever arquivo de áudio usando Gemini
     * 
     * @param audioFilePath Caminho para o arquivo de áudio local
     * @return Texto da transcrição
     */
    public String transcribeAudio(String audioFilePath) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("GEMINI_API_KEY não configurada");
        }

        String fileUri = uploadToGemini(audioFilePath);

        ObjectNode request = buildGenerateContentRequest(
                "Gere uma transcrição detalhada deste áudio. Inclua todas as palavras faladas com precisão. " +
                "Retorne APENAS a transcrição, sem introduções ou textos explicativos.",
                fileUri);

        JsonNode response = callGenerateContent(request);
        String rawTranscription = extractTextFromResponse(response);
        
        return cleanTranscription(rawTranscription);
    }
    
    /**
     * Limpar transcrição removendo textos introdutórios
     */
    private String cleanTranscription(String transcription) {
        if (transcription == null) return null;
        
        // Remove textos introdutórios comuns
        transcription = transcription.replaceFirst("(?i)^.*?transcrição.*?áudio[:\\s]*", "");
        transcription = transcription.replaceFirst("(?i)^.*?início da transcrição.*?$", "");
        transcription = transcription.replaceFirst("(?i)fim da transcrição.*?$", "");
        
        // Remove linhas em branco extras
        transcription = transcription.replaceAll("(?m)^\\s*$\\n", "");
        transcription = transcription.trim();
        
        return transcription;
    }

    /**
     * Analisar transcrição de ligação de vendas
     * 
     * @param transcription Texto da transcrição da ligação
     * @return Resultado da análise com dados estruturados
     */
    public GeminiAnalysisResult analyzeCall(String transcription) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("GEMINI_API_KEY não configurada");
        }

        String prompt = buildAnalysisPrompt(transcription);

        ObjectNode request = buildGenerateContentRequest(prompt, null);
        JsonNode response = callGenerateContent(request);
        String analysisJson = extractTextFromResponse(response);

        return parseAnalysisResult(analysisJson);
    }

    /**
     * Construir prompt de análise para saída estruturada
     */
    private String buildAnalysisPrompt(String transcription) {
        return String.format(
                """
                        Analise esta transcrição de ligação de vendas e retorne APENAS um JSON válido com a seguinte estrutura:

                        {
                          "resumo": "Resumo breve da ligação (máx 200 caracteres)",
                          "pontosFortes": ["ponto forte 1", "ponto forte 2", "ponto forte 3"],
                          "pontosFracos": ["ponto fraco 1", "ponto fraco 2", "ponto fraco 3"],
                          "sugestoes": ["sugestão 1", "sugestão 2", "sugestão 3"],
                          "sentimentScore": 75,
                          "probabilidadeFechamento": 60,
                          "categoriaAmbiental": "POSITIVO",
                          "qualidadeAtendimento": 85,
                          "aderenciaScript": 70,
                          "gestaoObjecoes": 65,
                          "objecoesIdentificadas": ["preço alto", "precisa consultar gerente"],
                          "momentosChave": ["00:45 - Cliente demonstrou interesse no produto premium", "02:30 - Objeção sobre preço foi contornada"]
                        }

                        Regras IMPORTANTES:
                        - sentimentScore: 0-100 (0=muito negativo, 100=muito positivo)
                        - probabilidadeFechamento: 0-100 (probabilidade de fechar venda)
                        - categoriaAmbiental: "POSITIVO", "NEUTRO" ou "NEGATIVO"
                        - pontosFortes: aspectos positivos da abordagem do vendedor
                        - pontosFracos: áreas de melhoria
                        - sugestoes: recomendações práticas
                        - qualidadeAtendimento: 0-100 (avalia cortesia, clareza, empatia e persuasão do vendedor)
                        - aderenciaScript: 0-100 (o quanto o vendedor seguiu o roteiro/script de vendas)
                        - gestaoObjecoes: 0-100 (eficácia em lidar com resistências e dúvidas do cliente)
                        - objecoesIdentificadas: array com as principais objeções/resistências levantadas pelo cliente
                        - momentosChave: SEMPRE use formato "MM:SS - descrição" (ex: "01:23 - Cliente aceitou proposta", "00:45 - Início da apresentação")

                        Transcrição:
                        %s

                        Retorne APENAS o JSON, sem markdown ou explicações adicionais.
                        """,
                transcription);
    }

    /**
     * Construir JSON de requisição para generateContent
     */
    private ObjectNode buildGenerateContentRequest(String textPrompt, String fileUri) {
        ObjectNode request = objectMapper.createObjectNode();
        ArrayNode contents = objectMapper.createArrayNode();
        ObjectNode content = objectMapper.createObjectNode();
        ArrayNode parts = objectMapper.createArrayNode();

        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("text", textPrompt);
        parts.add(textPart);

        if (fileUri != null) {
            ObjectNode filePart = objectMapper.createObjectNode();
            ObjectNode fileData = objectMapper.createObjectNode();
            fileData.put("file_uri", fileUri);
            filePart.set("file_data", fileData);
            parts.add(filePart);
        }

        content.set("parts", parts);
        contents.add(content);
        request.set("contents", contents);

        return request;
    }

    /**
     * Chamar API generateContent do Gemini
     */
    private JsonNode callGenerateContent(ObjectNode requestBody)
            throws IOException, InterruptedException {

        String url = GEMINI_API_BASE + "/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Erro na API Gemini: " + response.statusCode() + " - " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    /**
     * Extrair texto da resposta do Gemini
     */
    private String extractTextFromResponse(JsonNode response) {
        try {
            JsonNode promptFeedback = response.get("promptFeedback");
            if (promptFeedback != null) {
                JsonNode blockReason = promptFeedback.get("blockReason");
                if (blockReason != null && !blockReason.asText().isEmpty()) {
                    String reason = blockReason.asText();
                    throw new RuntimeException("Conteúdo bloqueado pela API do Gemini: " + reason + 
                        ". O áudio pode conter conteúdo inapropriado ou estar em formato não suportado.");
                }
            }
            
            JsonNode candidates = response.get("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);
                
                // Verifica se o candidate foi bloqueado
                JsonNode finishReason = candidate.get("finishReason");
                if (finishReason != null) {
                    String reason = finishReason.asText();
                    if ("SAFETY".equals(reason) || "PROHIBITED_CONTENT".equals(reason)) {
                        throw new RuntimeException("Conteúdo bloqueado por política de segurança do Gemini");
                    }
                }
                
                JsonNode content = candidate.get("content");
                if (content == null) {
                    throw new RuntimeException("Campo 'content' não encontrado na resposta");
                }
                
                JsonNode parts = content.get("parts");
                if (parts != null && parts.size() > 0) {
                    JsonNode text = parts.get(0).get("text");
                    if (text != null) {
                        return text.asText();
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Falha ao fazer parse da resposta do Gemini: " + e.getMessage(), e);
        }
        throw new RuntimeException("Nenhum texto encontrado na resposta do Gemini");
    }

    /**
     * Fazer parse do JSON de análise para objeto de resultado
     */
    private GeminiAnalysisResult parseAnalysisResult(String analysisJson) {
        try {
            analysisJson = analysisJson.trim();
            if (analysisJson.startsWith("```json")) {
                analysisJson = analysisJson.substring(7);
            }
            if (analysisJson.startsWith("```")) {
                analysisJson = analysisJson.substring(3);
            }
            if (analysisJson.endsWith("```")) {
                analysisJson = analysisJson.substring(0, analysisJson.length() - 3);
            }
            analysisJson = analysisJson.trim();

            JsonNode json = objectMapper.readTree(analysisJson);
            GeminiAnalysisResult result = new GeminiAnalysisResult();

            result.resumo = json.has("resumo") ? json.get("resumo").asText() : "Análise não disponível";
            result.sentimentScore = json.has("sentimentScore") ? json.get("sentimentScore").asInt() : 50;
            result.probabilidadeFechamento = json.has("probabilidadeFechamento")
                    ? json.get("probabilidadeFechamento").asInt()
                    : 50;

            if (json.has("categoriaAmbiental")) {
                try {
                    String categoria = json.get("categoriaAmbiental").asText();
                    result.categoriaAmbiental = CategoriaAmbiental.valueOf(categoria);
                } catch (IllegalArgumentException e) {
                    result.categoriaAmbiental = CategoriaAmbiental.NEUTRO;
                }
            } else {
                result.categoriaAmbiental = CategoriaAmbiental.NEUTRO;
            }

            result.qualidadeAtendimento = json.has("qualidadeAtendimento") ? json.get("qualidadeAtendimento").asInt()
                    : null;
            result.aderenciaScript = json.has("aderenciaScript") ? json.get("aderenciaScript").asInt() : null;
            result.gestaoObjecoes = json.has("gestaoObjecoes") ? json.get("gestaoObjecoes").asInt() : null;

            result.pontosFortes = parseStringArray(json, "pontosFortes");
            result.pontosFracos = parseStringArray(json, "pontosFracos");
            result.sugestoes = parseStringArray(json, "sugestoes");
            result.objecoesIdentificadas = parseStringArray(json, "objecoesIdentificadas");
            result.momentosChave = normalizeTimestamps(parseStringArray(json, "momentosChave"));

            return result;
        } catch (Exception e) {
            System.err.println("Falha ao fazer parse do JSON de análise: " + e.getMessage());
            System.err.println("JSON: " + analysisJson);
            return new GeminiAnalysisResult();
        }
    }

    /**
     * Helper para fazer parse de arrays de strings do JSON
     */
    private String[] parseStringArray(JsonNode json, String fieldName) {
        if (!json.has(fieldName)) {
            return new String[0];
        }

        JsonNode array = json.get(fieldName);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            list.add(array.get(i).asText());
        }
        return list.toArray(new String[0]);
    }

    /**
     * Normaliza timestamps em momentos-chave para formato MM:SS
     * Converte formatos como "1:23", "01:23", "0:05" para "01:23", "01:23", "00:05"
     */
    private String[] normalizeTimestamps(String[] momentos) {
        if (momentos == null) return new String[0];
        
        String[] normalized = new String[momentos.length];
        for (int i = 0; i < momentos.length; i++) {
            String momento = momentos[i];
            if (momento.matches("^\\d{1,2}:\\d{2}.*")) {
                String[] parts = momento.split(":", 2);
                int minutes = Integer.parseInt(parts[0]);
                String rest = parts[1];
                normalized[i] = String.format("%02d:%s", minutes, rest);
            } else {
                normalized[i] = momento;
            }
        }
        return normalized;
    }

    public static class GeminiAnalysisResult {
        public String resumo = "Análise pendente";
        public String[] pontosFortes = new String[0];
        public String[] pontosFracos = new String[0];
        public String[] sugestoes = new String[0];
        public int sentimentScore = 50;
        public int probabilidadeFechamento = 50;
        public CategoriaAmbiental categoriaAmbiental = CategoriaAmbiental.NEUTRO;
        public Integer qualidadeAtendimento = null;
        public Integer aderenciaScript = null;
        public Integer gestaoObjecoes = null;
        public String[] objecoesIdentificadas = new String[0];
        public String[] momentosChave = new String[0];
    }
}
