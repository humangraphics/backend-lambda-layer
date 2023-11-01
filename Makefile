all: layer.zip

layer.zip: target/layer.jar lambda-exec-wrapper.sh
	mkdir -p humangraphics/
	cd humangraphics && unzip ../target/layer.jar && cd -
	cp lambda-exec-wrapper.sh humangraphics/
	zip -r layer.zip humangraphics/

target/layer.jar:
	mvn clean compile package

clean:
	mvn clean
	rm -rf layer.zip humangraphics/
