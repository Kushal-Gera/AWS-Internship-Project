import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;

@SuppressWarnings("deprecation")
public class ImplementMethods {
	
	String keyArn = UserDetails.CMK_KEY_ARN;
    String clientRegion = UserDetails.CLIENT_REGION;
    String bucketName = UserDetails.BUCKET_NAME;
    String objectKeyName = UserDetails.FILE_NAME;
    
	 public void uploadFile(AWSCredentials credentials) throws NoSuchAlgorithmException, IOException {
		 
	        SecretKey symKey_file = keyGenerator(); 
	        
	        String encodedKey = encodeKey(symKey_file);
	      
	        //Encryption using Generated Symmetric key
	        EncryptionMaterials encryptionMaterials = new EncryptionMaterials(symKey_file);
	        AmazonS3 s3EncryptionClient_file = AmazonS3EncryptionClientBuilder.standard()
	                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
	                    .withEncryptionMaterials(new StaticEncryptionMaterialsProvider(encryptionMaterials))
	                    .withRegion(clientRegion)
	                    .build();

	        //Creates a new bucket if the given bucket does not exist
	        if (!s3EncryptionClient_file.doesBucketExistV2(bucketName))
	                s3EncryptionClient_file.createBucket(bucketName);


	        byte[] file_data = FileUtils.readFileToByteArray(new File(UserDetails.FILE_PATH));
	            
	        s3EncryptionClient_file.putObject(new PutObjectRequest(
	                    bucketName,
	                    objectKeyName,
	                    new ByteArrayInputStream(file_data),
	                    new ObjectMetadata()));
	            


	        System.out.println("File encrypted and uploaded into the S3 bucket");
	        
	        uploadKey(encodedKey, credentials);
	        
	        SnsUpload(credentials);     
	    }	
	
	// Generate a symmetric 256-bit AES key.
	public SecretKey keyGenerator() throws NoSuchAlgorithmException {
		KeyGenerator symKeyGenerator = KeyGenerator.getInstance("AES");
		symKeyGenerator.init(256);
		SecretKey symKey = symKeyGenerator.generateKey();
		return symKey;
		
	}
	
	//Used to convert Secret Key to a String (by encoding) to upload to the S3 bucket
	public String encodeKey(SecretKey symKey) {
		String encodedKey = Base64.getEncoder().encodeToString(symKey.getEncoded());
		return encodedKey;
	}

	public void uploadKey(String encodedKey, AWSCredentials credentials) {
		AWSKMS kmsClient = AWSKMSClientBuilder.standard()
				.withRegion(clientRegion)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();

		String s3ObjectKeyName = objectKeyName + "Key";
		String s3ObjectContent = encodedKey;

		AmazonS3 s3EncryptionClient_key = AmazonS3EncryptionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withEncryptionMaterials(new KMSEncryptionMaterialsProvider(keyArn))
				.withRegion(clientRegion)
				.withKmsClient(kmsClient)
				.build();


		s3EncryptionClient_key.putObject(bucketName, s3ObjectKeyName, s3ObjectContent);

		System.out.println("Key encrypted and uploaded into S3 bucket");
	}
	
	public void SnsUpload(AWSCredentials credentials) {
		AmazonSNS snsClient = AmazonSNSClient.builder()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(clientRegion).build();

		// Create an Amazon SNS topic.
		CreateTopicRequest createTopicRequest = new CreateTopicRequest(UserDetails.BUCKET_NAME + "_notify");
		CreateTopicResult createTopicResponse = snsClient.createTopic(createTopicRequest);
		UserDetails.TOPIC_ARN = createTopicResponse.getTopicArn();

		snsClient.publish(UserDetails.TOPIC_ARN, UserDetails.EMAIL_MESSAGE_UPLOAD, UserDetails.EMAIL_SUBJECT_UPLOAD);
	}
	
	public void downloadFile(AWSCredentials credentials) throws IOException {


		String encodedKey = downloadKey(credentials);
		SecretKey symKey_file = decodeKey(encodedKey);        


		EncryptionMaterials encryptionMaterials = new EncryptionMaterials(symKey_file);
		AmazonS3 s3EncryptionClient_file = AmazonS3EncryptionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withEncryptionMaterials(new StaticEncryptionMaterialsProvider(encryptionMaterials))
				.withRegion(clientRegion)
				.build();


		S3Object downloadedObject = s3EncryptionClient_file.getObject(bucketName, objectKeyName);
		S3ObjectInputStream inputStream = downloadedObject.getObjectContent();
		FileUtils.copyInputStreamToFile(inputStream, new File(UserDetails.LOCAL_DOWNLOAD_PATH));
		System.out.println("File Downloaded Successfully");


		SnsDownload(credentials);	

	}
	
	public String downloadKey(AWSCredentials credentials) {
		AWSKMS kmsClient = AWSKMSClientBuilder.standard()
        		.withRegion(clientRegion)
        		.withCredentials(new AWSStaticCredentialsProvider(credentials))
                .build();
        

        
        AmazonS3 s3EncryptionClient_key = AmazonS3EncryptionClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEncryptionMaterials(new KMSEncryptionMaterialsProvider(keyArn))
                .withRegion(clientRegion)
                .withKmsClient(kmsClient)
                .build();
        
        String encodedKey = s3EncryptionClient_key.getObjectAsString(bucketName, objectKeyName+"Key");
        return encodedKey;
		
	}
	
	public SecretKey decodeKey(String encodedKey) {
		 byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
	     SecretKey symKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
	     return symKey;		
	}
 
	public void SnsDownload(AWSCredentials credentials) {
		AmazonSNS snsClient = AmazonSNSClient.builder()
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withRegion(clientRegion).build();

		// Create an Amazon SNS topic.
		CreateTopicRequest createTopicRequest = new CreateTopicRequest(UserDetails.BUCKET_NAME + "_notify");
		CreateTopicResult createTopicResponse = snsClient.createTopic(createTopicRequest);
		UserDetails.TOPIC_ARN = createTopicResponse.getTopicArn();

		snsClient.publish(UserDetails.TOPIC_ARN, UserDetails.EMAIL_MESSAGE_DOWNLOAD, UserDetails.EMAIL_SUBJECT_DOWNLOAD);
	}
}
