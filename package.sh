rm -rf ./release
mkdir -p ./release/static
mkdir -p ./release/data
cp -pr ./src/main/webapp/WEB-INF/client ./release
cp -pr ./src/main/webapp/i ./release/static/
cp -pr ./src/main/webapp/css ./release/static/
cp -pr ./src/main/webapp/js ./release/static/
cp -pr ./src/main/webapp/html ./release/static/
./sbt assembly
cp ./target/scala-*/Altitude-assembly-0.1.0-SNAPSHOT.jar ./release/
