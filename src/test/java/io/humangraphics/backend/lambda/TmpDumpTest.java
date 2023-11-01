package io.humangraphics.backend.lambda;

import java.io.IOException;
import org.junit.Test;
import com.sigpwned.httpmodel.aws.AwsSigningCredentials;

// @Ignore
public class TmpDumpTest {
  @Test
  public void test() throws IOException {
    final String accessKeyId = "ASIAVSBVFUDXE2XQ6LZG";
    final String secretAccessKey = "f/RK9pgdZIEWE6JZIIU1FW5lqEcrZhCEcGFS3GSw";
    final String sessionToken =
        "IQoJb3JpZ2luX2VjEOD//////////wEaCXVzLWVhc3QtMiJHMEUCIGOQLQlosemW5Vr9E+yGbycGLBOuOGsYKj4oPqJVMIrBAiEAh0Eu7PJJO5KjDc0LSjjsBN0AdsH6cZEkLFDPSreq2E4qpQMIGRAAGgwzODIzNjM2MDcyNzgiDJxRdV0P3dBHHnRGEyqCAzI9cG/Ykhf/OojJFBC6htieD0Kzcj60Tns/fjiDDcYVF+2sAj8SmylQPbRN+6G+vjRQvqUishT7rjxxPyl1nn1lwmbfRN4e63BD6jP0S7gj0Nl3hUmKU0IOrW78YWlQyCo3P8mAa9i3YsKsk40gS3KK+XWChToknwUmtJnZsc3xbcCcKXN2cVeIjtQ65M+9NzS/OUg5HY5qdgz8aBEN+yPYey5djdle0VCSjT5VZjSGPrZIyXH0HvzBvZmUyp7pP5CJKvVni75eW6W7TQvHCt4O76r8iLWj24l+OHtCI9AvwAY4vz+u7F8G2Bm6ohH8f/0gws7efUDhhpZmBAnT3KiixiwzvreFjUaxBX9xUZ8cgVTuoVMOgigl3B378IgIDyY6Z7o9z2zA2ShWz1CBA7w/suCxHF9B2G4ykhSCsBPsekYFAjSy8HEF4U/iXo4pOyG2m2jzsyHk3CC2xL1gnPF6zbvSQQ1H7FRqqWAigakaP1/lxgR9CBOKQe0XwOUE+0qTMOHqiaoGOp0Bi1Jjhsh6gfKav+4VK3ZhUrVBjyPKHnQnKhv6XEz2ynnrXNai/gr6HjFy0NZ4E85im4WVK8prxR6fVyv9otS2jdeBTlCyTX7QxzxhyzBqDqoRAAf+1jFoqwYF4/gMtPc00iARofYsyDGj1oiGqUq3r+b6NlsEkPVZXBXYX8Pj+rUnvjYfFOiKErAfaSfgvQBUOqCX3qyPRWP23Dg0jA==";
    TmpDump.s3get(() -> AwsSigningCredentials.of(accessKeyId, secretAccessKey, sessionToken),
        "s3://humangraphics-382363607278-us-east-2/tmpdump/humangraphics-backend-detect-human-faces.zip",
        "humangraphics-382363607278-us-east-2",
        "/tmpdump/humangraphics-backend-detect-human-faces.zip", "us-east-2");
  }
}
