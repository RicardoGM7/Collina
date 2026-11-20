package br.ufs.dcomp.collina.config;

import com.google.api.client.googleapis.util.Utils;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

// Inicializa o Firebase Admin SDK com a conta de serviço do projeto
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(@Value("${firebase.credentials-path:}") String credentialsPath,
                                   @Value("${firebase.project-id:}") String projectId) {
        // Evita inicializar duas vezes (ex.: restart do devtools)
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials;
        try {
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                try (InputStream in = new FileInputStream(credentialsPath)) {
                    credentials = GoogleCredentials.fromStream(in);
                }
            } else {
                credentials = GoogleCredentials.getApplicationDefault();
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Credenciais do Firebase não encontradas. Defina FIREBASE_CREDENTIALS_PATH com o caminho do JSON "
                            + "da conta de serviço (Firebase Console > Configurações do projeto > Contas de serviço). "
                            + "Veja a seção 'Autenticação' do README.", e);
        }

        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setHttpTransport(Utils.getDefaultTransport());
        if (projectId != null && !projectId.isBlank()) {
            builder.setProjectId(projectId);
        }
        return FirebaseApp.initializeApp(builder.build());
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
