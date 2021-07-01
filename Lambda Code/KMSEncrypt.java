package com.example;
import java.net.URLDecoder;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;


public class LambdaTrigger implements RequestHandler<S3EventNotification, String> {

	String ACCESS_KEY = "";
	String SECRET_KEY = "";
	String kms_cmk_keyID = "";
	String CLIENT_REGION = "ap-south-1";
	String DEST_BUCKET_NAME = "files-encrypted";

	@Override
	public String handleRequest(S3EventNotification s3Event, Context context) {

        try {
    		System.out.println("Lambda function is invoked");
        	
    		S3EventNotificationRecord record = s3Event.getRecords().get(0);
            String bucketName = record.getS3().getBucket().getName();
            String keyName = record.getS3().getObject().getKey().replace('+', ' ');
        	keyName = URLDecoder.decode(keyName, "UTF-8");
        	
			AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);

			KMSEncryptionMaterialsProvider materialProvider = new KMSEncryptionMaterialsProvider(kms_cmk_keyID);
	        CryptoConfiguration cryptoConfig = new CryptoConfiguration()
	                .withAwsKmsRegion(RegionUtils.getRegion(CLIENT_REGION));
			
	        AmazonS3 s3EncryptionClient = AmazonS3EncryptionClientBuilder.standard()
	                .withCredentials(new AWSStaticCredentialsProvider(credentials))
	                .withEncryptionMaterials(materialProvider)
	                .withCryptoConfiguration(cryptoConfig)
	                .withRegion(CLIENT_REGION).build();
	        
			System.out.println("Clients Ready");
			
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().
					 withCredentials(new AWSStaticCredentialsProvider(credentials))
					 .withRegion(CLIENT_REGION).build();
			
			S3Object downloadedObject = s3Client.getObject(bucketName, keyName);
			S3ObjectInputStream inputStream = downloadedObject.getObjectContent();
			ObjectMetadata metadata = downloadedObject.getObjectMetadata();

			System.out.println("Object Ready");
	        
			s3EncryptionClient.putObject(
					new PutObjectRequest(DEST_BUCKET_NAME, keyName, inputStream, metadata));
          
			System.out.println("File encrypted and uploaded into the S3 bucket");
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}

		return null;
	}
}
