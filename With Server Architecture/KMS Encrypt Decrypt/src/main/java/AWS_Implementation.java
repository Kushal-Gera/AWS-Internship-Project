import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class AWS_Implementation implements AWS_Interface {

    @Override
    public void upload(AWSCredentials credentials, byte[] data) {

        KMSEncryptionMaterialsProvider materialProvider = new KMSEncryptionMaterialsProvider(kms_cmk_keyID);
        CryptoConfiguration cryptoConfig = new CryptoConfiguration()
                .withAwsKmsRegion(RegionUtils.getRegion(clientRegion));

        AmazonS3 s3EncryptionClient = AmazonS3EncryptionClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEncryptionMaterials(materialProvider)
                .withCryptoConfiguration(cryptoConfig)
                .withRegion(clientRegion).build();

        if (!s3EncryptionClient.doesBucketExistV2(bucketName))
            s3EncryptionClient.createBucket(bucketName);

        s3EncryptionClient.putObject(
                new PutObjectRequest(bucketName, objectKeyName, new ByteArrayInputStream(data), new ObjectMetadata()));
        System.out.println("Encrypted Successfully");
    }

    @Override
    public void download(AWSCredentials credentials) throws IOException {
        KMSEncryptionMaterialsProvider materialProvider = new KMSEncryptionMaterialsProvider(kms_cmk_keyID);

        CryptoConfiguration cryptoConfig = new CryptoConfiguration()
                .withAwsKmsRegion(RegionUtils.getRegion(clientRegion));
        AmazonS3 s3EncryptionClient = AmazonS3EncryptionClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEncryptionMaterials(materialProvider)
                .withCryptoConfiguration(cryptoConfig)
                .withRegion(clientRegion).build();

        System.out.println("Decrypted Successfully");
        S3Object downloadedObject = s3EncryptionClient.getObject(bucketName, objectKeyName);
        S3ObjectInputStream inputStream = downloadedObject.getObjectContent();
        FileUtils.copyInputStreamToFile(inputStream, new File(MyConstants.LOCAL_DOWNLOAD_PATH));
    }

    @Override
    public void notify(AWSCredentials credentials, String message, String subject) {
        AmazonSNS snsClient = AmazonSNSClient.builder()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(clientRegion).build();

        CreateTopicRequest createTopicRequest = new CreateTopicRequest(MyConstants.BUCKET_NAME + "_notify");
        CreateTopicResult createTopicResponse = snsClient.createTopic(createTopicRequest);
        MyConstants.TOPIC_ARN = createTopicResponse.getTopicArn();

        snsClient.publish(MyConstants.TOPIC_ARN, message, subject);
    }
}
