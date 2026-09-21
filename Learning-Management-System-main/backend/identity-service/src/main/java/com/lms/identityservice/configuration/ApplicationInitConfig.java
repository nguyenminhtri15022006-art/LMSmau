package com.lms.identityservice.configuration;

import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RoleType;
import com.lms.identityservice.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {
    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    MinioClient minioClient;

    @Value("${minio.bucket-name}")
    @NonFinal
    String bucketName;

    @Value("${app.bootstrap-admin.enabled:true}")
    @NonFinal
    boolean bootstrapAdminEnabled;

    @Value("${app.bootstrap-admin.usernames:admin}")
    @NonFinal
    String[] adminUsernames;

    @Value("${app.bootstrap-admin.passwords:admin}")
    @NonFinal
    String[] adminPasswords;

    @Value("${app.bootstrap-admin.emails:}")
    @NonFinal
    String[] adminEmails;

    @Value("${app.bootstrap-admin.fullnames:}")
    @NonFinal
    String[] adminFullnames;

    @Value("${app.bootstrap-admin.email-domain:lms.edu.vn}")
    @NonFinal
    String adminEmailDomain;

    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driver-class-name",
            havingValue = "com.mysql.cj.jdbc.Driver")
    ApplicationRunner applicationRunner() {
        log.info("Initializing Application ...");
        return args -> {
            initMinioBucket();
            initAdminUsers();
            log.info("Initializing Application completed ...");
        };
    }

    private void initMinioBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' created successfully.", bucketName);

            } else {
                log.info("MinIO bucket '{}' already exists.", bucketName);
            }
            applyPublicReadPolicy();
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket '{}': {}", bucketName, e.getMessage(), e);
        }


    }

    private void applyPublicReadPolicy() throws Exception {
        String policyJson = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucketName);

        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(policyJson)
                        .build());

        log.info("Public-read policy successfully applied to '{}'.", bucketName);
    }

    private void initAdminUsers() {
        if (!bootstrapAdminEnabled) {
            log.info("Bootstrap admin creation is disabled.");
            return;
        }

        if (adminUsernames.length != adminPasswords.length) {
            log.error("Bootstrap admin usernames and passwords must have the same length");
            return;
        }

        for (int i = 0; i < adminUsernames.length; i++) {
            String username = adminUsernames[i].trim();
            String password = adminPasswords[i];

            if (username.isBlank() || password == null || password.isBlank()) {
                log.error("Bootstrap admin at index {} has blank username or password, skipping.", i);
                continue;
            }

            if (userRepository.findByUsername(username).isPresent()) {
                log.info("Admin user '{}' already exists, skipping.", username);
                continue;
            }

            User user = User.builder()
                    .username(username)
                    .email(getOptionalValue(adminEmails, i, username + "@" + adminEmailDomain))
                    .fullname(getOptionalValue(adminFullnames, i, "Admin " + username))
                    .password(passwordEncoder.encode(password))
                    .role(RoleType.ADMIN)
                    .roleSelected(true)
                    .verified(true)
                    .authProvider(AuthProvider.LOCAL)
                    .build();

            userRepository.save(user);
            log.warn("Admin user '{}' created with default password. Please change it immediately.", username);
        }
    }

    private String getOptionalValue(String[] values, int index, String defaultValue) {
        if (values == null || index >= values.length || values[index] == null || values[index].isBlank()) {
            return defaultValue;
        }
        return values[index].trim();
    }
}
