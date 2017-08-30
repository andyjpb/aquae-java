.PHONY: all protobufs clean mrproper

PROTOBUF_RUNTIME=/usr/share/java/protobuf-2.6.1.jar
LIBAQUAE=../libAquae/libAquae-0.1.0.jar
DEPENDENCIES=${LIBAQUAE}:${PROTOBUF_RUNTIME}

PROTOBUFS=$(wildcard *.proto)
PROTOBUFS_SENTINELS=${PROTOBUFS:%.proto=generated/.%.sentinel}
GENERATED_JAVA=$(shell if [ -d generated/ ]; then find generated/ -type f -name \*.java; fi)

SOURCE_JAVA=$(wildcard *.java)

VERSION=0.1.0
VIADUCTD=ViaductD-${VERSION}.jar

all: ${VIADUCTD}

${VIADUCTD}: ${SOURCE_JAVA} | protobufs
	javac -source 1.5 -target 1.5 -d bin/ -cp bin/:${DEPENDENCIES} ${SOURCE_JAVA}
	cd bin/; jar cf ../$@ `find -name \*.class | sort`

protobufs: generated/ ${PROTOBUFS_SENTINELS} bin/.protobufs.sentinel

clean:
	rm -f ${VIADUCTD}
	rm -f generated/.*.sentinel bin/.protobufs.sentinel
	if [ -d generated/ ]; then find generated/ -name \*.java  | xargs -r rm ; fi
	if [ -d bin/       ]; then find bin/       -name \*.class | xargs -r rm ; fi
	if [ -d generated/ ]; then find generated/ -type d | sort -r | xargs -r rmdir --ignore-fail-on-non-empty ; fi
	if [ -d bin/       ]; then find bin/       -type d | sort -r | xargs -r rmdir --ignore-fail-on-non-empty ; fi

mrproper: clean
	rm -fr ViaductD-*.jar
	rm -fr bin/ generated/
	rm -f *~

generated/ bin/:
	mkdir $@

# Causes the protobuf .java files to be updated when their .proto files change.
generated/.%.sentinel : %.proto | generated/
	protoc --java_out=generated/ $<
	touch $@

# Causes the protobuf .class files to be updates when their .java files change.
bin/.protobufs.sentinel : ${GENERATED_JAVA} | bin/
	javac -source 1.5 -target 1.5 -d bin/ -cp ${DEPENDENCIES} ${GENERATED_JAVA}
	touch $@

