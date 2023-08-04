package com.etri.sodasapi.utils;

import com.etri.sodasapi.config.Constants;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class S3Signature {

    private final Constants constants;
    static final String SECRET_KEY;
    static final String SESSION_TOKEN = "<session_token>";
    static final String REGION = "<region>";  //ex. eu-west-1
    static final String AWS_SERVICE = "<service>";  //ex.  s3
    static final String AMZ_DATE = "<value of X-Amz-Date header>"; //ex. 20220804T123312Z
    static final String HOST = "<host>";
    static String BUCKET = "<bucket>";
    static String OBJECT_KEY = "<object key>";
    static final String HTTP_METHOD = "<Http Method>"; //ex.   GET|POST|PUT
    //uri example   https://host/bucket/object_key

    public static void main(String[] args) throws Exception {
        String canonicalRequest = createCanonicalRequest();
        System.out.println("canonicalRequest : \n" + canonicalRequest);
        System.out.println("--------------------------------------------------------------");
        String stringToSign = createStringToSign(canonicalRequest);
        System.out.println("stringToSign : \n" + stringToSign);
        System.out.println("--------------------------------------------------------------");
        String signature = calculateSignature(stringToSign);
        System.out.println("signature : \n" + signature);
    }


    public static String createCanonicalRequest() throws NoSuchAlgorithmException {
        /*
        * <HTTPMethod>\n
          <CanonicalURI>\n
          <CanonicalQueryString>\n
          <CanonicalHeaders>\n
          <SignedHeaders>\n
          <HashedPayload>
        * */
        BUCKET = BUCKET.isEmpty() ? BUCKET : "/" + BUCKET;
        OBJECT_KEY = "/" + OBJECT_KEY; // object key shall not be empty
        String canonicalURI = BUCKET + OBJECT_KEY; //to encode characters such as "+", "*", " ". "/" does not have to be encoded
        String canonicalQueryString = "<encoded canonical query string>";
        List<String> lowercasedTrimmedheaders = new ArrayList<>();//headers to add: host, content-type(if present), all x-amz-* headers.
        //to be lower-case'd and ordered alphabetically
        lowercasedTrimmedheaders.add("host" + ":" + HOST.toLowerCase(Locale.ROOT).trim());
        String hashedPayload = hex(getSHA("<payload>")).trim();
        lowercasedTrimmedheaders.add("x-amz-content-sha256" + ":" + hashedPayload); //if there's no payload, hashed empty string
        lowercasedTrimmedheaders.add("x-amz-date" + ":" + AMZ_DATE.trim());
        lowercasedTrimmedheaders.add("x-amz-security-token" + ":" + SESSION_TOKEN.trim());
        String canonicalHeaders = String.join("\n", lowercasedTrimmedheaders) + "\n";
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date;x-amz-security-token";
        return HTTP_METHOD
                + "\n"
                + canonicalURI
                + "\n"
                + canonicalQueryString
                + "\n"
                + canonicalHeaders
                + "\n"
                + signedHeaders
                + "\n"
                + hashedPayload;
    }

    public static String createStringToSign(String canonicalRequest) throws NoSuchAlgorithmException {
        /*
        * "AWS4-HMAC-SHA256" + "\n" +
           timeStampISO8601Format + "\n" +
           <Scope> + "\n" +
           Hex(SHA256Hash(<CanonicalRequest>))
        * */
        String hashAlgorithm = "AWS4-HMAC-SHA256";
        String YYYYMMDDdate = AMZ_DATE.substring(0, 8);
        String scope = YYYYMMDDdate + "/" + REGION + "/" + AWS_SERVICE + "/aws4_request";
        String canonicalRequestHex = hex(getSHA(canonicalRequest));
        return hashAlgorithm
                + "\n"
                + AMZ_DATE
                + "\n"
                + scope
                + "\n"
                + canonicalRequestHex;
    }

    public static String calculateSignature(String stringToSign) throws Exception {
        /*
         * DateKey              = HMAC-SHA256("AWS4"+"<SecretAccessKey>", "<YYYYMMDD>")
         * DateRegionKey        = HMAC-SHA256(<DateKey>, "<aws-region>")
         * DateRegionServiceKey = HMAC-SHA256(<DateRegionKey>, "<aws-service>")
         * SigningKey           = HMAC-SHA256(<DateRegionServiceKey>, "aws4_request")
         */
        byte[] kSecret = ("AWS4" + SECRET_KEY).getBytes(StandardCharsets.UTF_8);
        byte[] dateKey = HmacSHA256(AMZ_DATE.substring(0, 8), kSecret);
        byte[] dateRegionKey = HmacSHA256(REGION, dateKey);
        byte[] dateRegionServiceKey = HmacSHA256(AWS_SERVICE, dateRegionKey);
        byte[] signingKey = HmacSHA256("aws4_request", dateRegionServiceKey);
        return hex(HmacSHA256(stringToSign, signingKey));
    }

    public static byte[] getSHA(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] HmacSHA256(String data, byte[] key) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString();
    }
}
