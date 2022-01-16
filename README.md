# StS Upload Workshop Plugin

Maven plugin to upload Slay the Spire mod to steam workshop.

## How to use

1. Clone
2. Maven install
3. Add following code to pom.xml of your mod:

```xml
<plugins>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-deploy-plugin</artifactId>
        <configuration>
            <skip>true</skip>
        </configuration>
    </plugin>
    <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-install-plugin</artifactId>
        <configuration>
            <skip>true</skip>
        </configuration>
    </plugin>
    <plugin>
        <groupId>io.chaofan.sts.mavenplugin</groupId>
        <artifactId>sts-upload-workshop-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
        <executions>
            <execution>
                <goals>
                    <goal>upload</goal>
                </goals>
            </execution>
        </executions>
        <configuration>
            <stsInstallPath>${Steam.path}/common/SlayTheSpire</stsInstallPath>
            <uploadModPath>{path/to/your/mod/upload/path/that/contains/config.json/}</uploadModPath>
        </configuration>
    </plugin>
</plugins>
```