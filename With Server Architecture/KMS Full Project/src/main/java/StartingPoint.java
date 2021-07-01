import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Encryption;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import org.apache.commons.io.FileUtils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;

public class StartingPoint {

    public static void main(String[] args) {

        AWS_Implementation aws_implementation = new AWS_Implementation();
        AWSCredentials credentials = new BasicAWSCredentials(MyConstants.ACCESS_KEY, MyConstants.SECRET_KEY);

        try {
            byte[] data = FileUtils.readFileToByteArray(new File(MyConstants.FILE_PATH));

            aws_implementation.upload(credentials, data);
//            aws_implementation.notify(credentials, "Object Uploaded", "Sub: Object Uploaded");
            System.out.println("Uploaded Successfully");

            aws_implementation.download(credentials);
            System.out.println("Downloaded Successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

