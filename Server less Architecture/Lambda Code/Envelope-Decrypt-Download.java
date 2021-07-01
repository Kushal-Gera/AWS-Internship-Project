package com.test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.KMSEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;


public class App implements RequestHandler<S3EventNotification, String> {

	
	Logger log = LoggerFactory.getLogger(App.class);
	final String ACCESS_KEY = "**************";
	final String SECRET_KEY = "**************";
	String CMK_KEY_ARN = "********************";
	String CLIENT_REGION = "ap-south-1";
	String BUCKET_NAME = "new-bucket-download";
	

		public String handleRequest(S3EventNotification s3Event, Context context) {
		log.info("Lambda function is invoked:" + s3Event.toJson());
		
		//Get the File details which triggered the Lambda Function
		S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().get(0);
        
        String keyName = record.getS3().getObject().getKey().replace('+', ' ');
        
			try {
				keyName = URLDecoder.decode(keyName, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			 AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
		       
					 
					 AmazonS3 s3Client = AmazonS3ClientBuilder.standard().
							 withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion(CLIENT_REGION).build();
					 
					AWSKMS kmsClient = AWSKMSClientBuilder.standard()
			        		.withRegion(CLIENT_REGION)
			        		.withCredentials(new AWSStaticCredentialsProvider(credentials))
			                .build();
					
					 AmazonS3 s3EncryptionClient_key = AmazonS3EncryptionClientBuilder.standard()
				                .withCredentials(new AWSStaticCredentialsProvider(credentials))
				                .withEncryptionMaterials(new KMSEncryptionMaterialsProvider(CMK_KEY_ARN))
				                .withRegion(CLIENT_REGION)
				                .withKmsClient(kmsClient)
				                .build();
					String encodedKey = s3EncryptionClient_key.getObjectAsString(BUCKET_NAME, keyName+"Key");
					 
					byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
				    SecretKey symKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
				    
				    
				    EncryptionMaterials encryptionMaterials = new EncryptionMaterials(symKey);
					AmazonS3 s3EncryptionClient_file = AmazonS3EncryptionClientBuilder.standard()
							.withCredentials(new AWSStaticCredentialsProvider(credentials))
							.withEncryptionMaterials(new StaticEncryptionMaterialsProvider(encryptionMaterials))
							.withRegion(CLIENT_REGION)
							.build();
					
					S3Object downloadedObject = s3EncryptionClient_file.getObject(BUCKET_NAME, keyName);
					S3ObjectInputStream inputStream = downloadedObject.getObjectContent();
					 
					s3Client.putObject("open-download-bucket", keyName, inputStream, new ObjectMetadata());
					return null;				 
					 
	}
}
