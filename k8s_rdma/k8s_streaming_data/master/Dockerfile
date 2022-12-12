FROM ubuntu:latest
ARG DEBIAN_FRONTEND=noninteractive # ignore user input required
# Install required build dependencies
RUN apt-get -y update && apt-get install -y
RUN apt-get -y install g++ cmake git libibverbs-dev
RUN ulimit -l
COPY . .
WORKDIR .
# Run cmake configure & build process
RUN mkdir build
RUN cd build
RUN cmake .
RUN make
# Launch built application
CMD ["./k8s_master_streaming_data"]
