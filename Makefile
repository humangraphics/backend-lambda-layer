all: layer.zip

layer.zip: lambda-exec-wrapper.sh
	zip layer.zip lambda-exec-wrapper.sh

clean:
	rm -f layer.zip
