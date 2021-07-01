import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;

import javax.crypto.SecretKey;
import java.io.IOException;


public interface AWS_Interface {

    String clientRegion = MyConstants.CLIENT_REGION;
    String bucketName = MyConstants.BUCKET_NAME;
    String objectKeyName = MyConstants.FILE_NAME;
    String kms_cmk_keyID = MyConstants.kms_cmk_keyID;

    void upload(AWSCredentials credentials, byte[] data) throws IOException;

    void download(AWSCredentials credentials) throws IOException;

    void notify(AWSCredentials credentials, String message, String subject);

}
