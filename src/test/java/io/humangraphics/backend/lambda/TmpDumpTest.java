package io.humangraphics.backend.lambda;

import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class TmpDumpTest {
  @Test
  public void test() throws IOException {
    TmpDump.s3get(
        "s3://humangraphics-382363607278-us-east-2/libdump/humangraphics-backend-detect-human-faces.zip",
        "humangraphics-382363607278-us-east-2",
        "/libdump/humangraphics-backend-detect-human-faces.zip", "ASIAVSBVFUDXP723GJ7U",
        "cSDqEuQXy/msuloQVlLCrnrW+S0/JdHdl1n4IFW/",
        "IQoJb3JpZ2luX2VjEMz//////////wEaCXVzLWVhc3QtMiJHMEUCIC4GZw0YNN3CpK5C6v4In+MsANFDaXGyhy/io7en4ix0AiEA0RQih5m8fI/XkyEvoqLTRrURRaaINXCtzCb041ZSB/EqrgMI9f//////////ARAAGgwzODIzNjM2MDcyNzgiDCJ9McDFGc6LPuB0dyqCAw5QLN9t2LjL+1JysR+31zPo1QBxH62l5Sn4yzZTkpjKiNCz7LgSG6mbcBH9bYYlIMB8X3yFE9ZDmiAcGqjbJS1B4loHn0jLftbfC7W6yPo0yas7U6Q8SWGz8pLhUebaw5d7sptULLPNjlr3ITryl9Si6poHctGTTmBgwMAI5KwUEj35ZgbFf6DsvvZiO6yA8u6OsKHnkGBfaQZOpskMwGplFzcV1G4LLoPdL3PVT4P7soL2FSD08Ca2feBJENom0hmt/bSPPNJuqSZTP688JMPn+jIqKNNodORZEfSSax9fLl/5WLdABKHRps3wISCBacPv+5LX9LLk9vCNBTDPmxCWc/5XK0MUJb88VrhTD68IiwbHIcDkNngyaywgH2hw2IbYf0SF9CwtYTfJDlldy861ti7PVld74x+FPhtK7Gsf0gG4mQjsuQPpA3cUo94/zXKTa5nWQqgeMWILdN97ovABKaB0HaAXLY2/c+CjcwwlLAY3k6rBqu/YIU8ehK7Q96znMPu7haoGOp0BFQMJmP2erpvrXWJMoHvfE9QeTPOWFhjMj2tgCHDsyJboqF2S7MiBJF8Cu6t3y1k/JlcBK5TDM5ZS6DkgCMDWPLQZV73Ir0A2qqFa8zOqqwL31AfDbR+eA/AE+6dVDukJV9U4avJ4qzHGedx1kL/05bLxjbpAcm/9MTGrScIphty6i9MD+28bYJQDFji551waQxrbzsz9vy1Vqvt4LQ==",
        "us-east-2");
  }
}
