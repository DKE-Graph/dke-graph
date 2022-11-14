FROM ubuntu:latest
ARG DEBIAN_FRONTEND=noninteractive # ignore user input required
# Install required build dependencies
RUN apt-get -y update && apt-get install -y
RUN apt-get -y install g++ cmake git libibverbs-dev
COPY . .
WORKDIR .
# Run cmake configure & build process
RUN mkdir build
RUN cd build
RUN cmake .
RUN make
# Launch built application
CMD ["./k8s_tcp_test","192.168.0.107"]
