package servicestubs.runserver;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: run_server_contract.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RunServiceGrpc {

  private RunServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "grpcservice_runserver.RunService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<servicestubs.runserver.SubmissionChunk,
      servicestubs.runserver.RequestId> getSubmeterMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "submeter",
      requestType = servicestubs.runserver.SubmissionChunk.class,
      responseType = servicestubs.runserver.RequestId.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<servicestubs.runserver.SubmissionChunk,
      servicestubs.runserver.RequestId> getSubmeterMethod() {
    io.grpc.MethodDescriptor<servicestubs.runserver.SubmissionChunk, servicestubs.runserver.RequestId> getSubmeterMethod;
    if ((getSubmeterMethod = RunServiceGrpc.getSubmeterMethod) == null) {
      synchronized (RunServiceGrpc.class) {
        if ((getSubmeterMethod = RunServiceGrpc.getSubmeterMethod) == null) {
          RunServiceGrpc.getSubmeterMethod = getSubmeterMethod =
              io.grpc.MethodDescriptor.<servicestubs.runserver.SubmissionChunk, servicestubs.runserver.RequestId>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "submeter"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.SubmissionChunk.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.RequestId.getDefaultInstance()))
              .setSchemaDescriptor(new RunServiceMethodDescriptorSupplier("submeter"))
              .build();
        }
      }
    }
    return getSubmeterMethod;
  }

  private static volatile io.grpc.MethodDescriptor<servicestubs.runserver.RequestId,
      servicestubs.runserver.ResultChunk> getGetresultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getresult",
      requestType = servicestubs.runserver.RequestId.class,
      responseType = servicestubs.runserver.ResultChunk.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<servicestubs.runserver.RequestId,
      servicestubs.runserver.ResultChunk> getGetresultMethod() {
    io.grpc.MethodDescriptor<servicestubs.runserver.RequestId, servicestubs.runserver.ResultChunk> getGetresultMethod;
    if ((getGetresultMethod = RunServiceGrpc.getGetresultMethod) == null) {
      synchronized (RunServiceGrpc.class) {
        if ((getGetresultMethod = RunServiceGrpc.getGetresultMethod) == null) {
          RunServiceGrpc.getGetresultMethod = getGetresultMethod =
              io.grpc.MethodDescriptor.<servicestubs.runserver.RequestId, servicestubs.runserver.ResultChunk>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getresult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.RequestId.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.ResultChunk.getDefaultInstance()))
              .setSchemaDescriptor(new RunServiceMethodDescriptorSupplier("getresult"))
              .build();
        }
      }
    }
    return getGetresultMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RunServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RunServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RunServiceStub>() {
        @java.lang.Override
        public RunServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RunServiceStub(channel, callOptions);
        }
      };
    return RunServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RunServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RunServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RunServiceBlockingStub>() {
        @java.lang.Override
        public RunServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RunServiceBlockingStub(channel, callOptions);
        }
      };
    return RunServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RunServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RunServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RunServiceFutureStub>() {
        @java.lang.Override
        public RunServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RunServiceFutureStub(channel, callOptions);
        }
      };
    return RunServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Cliente envia o programa e recebe o UUID
     * </pre>
     */
    default io.grpc.stub.StreamObserver<servicestubs.runserver.SubmissionChunk> submeter(
        io.grpc.stub.StreamObserver<servicestubs.runserver.RequestId> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSubmeterMethod(), responseObserver);
    }

    /**
     * <pre>
     * Cliente consulta o resultado
     * </pre>
     */
    default void getresult(servicestubs.runserver.RequestId request,
        io.grpc.stub.StreamObserver<servicestubs.runserver.ResultChunk> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetresultMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RunService.
   */
  public static abstract class RunServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RunServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RunService.
   */
  public static final class RunServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RunServiceStub> {
    private RunServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RunServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RunServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Cliente envia o programa e recebe o UUID
     * </pre>
     */
    public io.grpc.stub.StreamObserver<servicestubs.runserver.SubmissionChunk> submeter(
        io.grpc.stub.StreamObserver<servicestubs.runserver.RequestId> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getSubmeterMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Cliente consulta o resultado
     * </pre>
     */
    public void getresult(servicestubs.runserver.RequestId request,
        io.grpc.stub.StreamObserver<servicestubs.runserver.ResultChunk> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetresultMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RunService.
   */
  public static final class RunServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RunServiceBlockingStub> {
    private RunServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RunServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RunServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Cliente consulta o resultado
     * </pre>
     */
    public java.util.Iterator<servicestubs.runserver.ResultChunk> getresult(
        servicestubs.runserver.RequestId request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetresultMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RunService.
   */
  public static final class RunServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RunServiceFutureStub> {
    private RunServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RunServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RunServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_GETRESULT = 0;
  private static final int METHODID_SUBMETER = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_GETRESULT:
          serviceImpl.getresult((servicestubs.runserver.RequestId) request,
              (io.grpc.stub.StreamObserver<servicestubs.runserver.ResultChunk>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SUBMETER:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.submeter(
              (io.grpc.stub.StreamObserver<servicestubs.runserver.RequestId>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSubmeterMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              servicestubs.runserver.SubmissionChunk,
              servicestubs.runserver.RequestId>(
                service, METHODID_SUBMETER)))
        .addMethod(
          getGetresultMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              servicestubs.runserver.RequestId,
              servicestubs.runserver.ResultChunk>(
                service, METHODID_GETRESULT)))
        .build();
  }

  private static abstract class RunServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RunServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return servicestubs.runserver.RunServerContract.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RunService");
    }
  }

  private static final class RunServiceFileDescriptorSupplier
      extends RunServiceBaseDescriptorSupplier {
    RunServiceFileDescriptorSupplier() {}
  }

  private static final class RunServiceMethodDescriptorSupplier
      extends RunServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RunServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (RunServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RunServiceFileDescriptorSupplier())
              .addMethod(getSubmeterMethod())
              .addMethod(getGetresultMethod())
              .build();
        }
      }
    }
    return result;
  }
}
