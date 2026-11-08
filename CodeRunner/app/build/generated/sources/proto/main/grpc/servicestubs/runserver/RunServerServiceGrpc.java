package servicestubs.runserver;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * Contrato c: Client -&gt; RunServer.
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.68.1)",
    comments = "Source: runserver_contract.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class RunServerServiceGrpc {

  private RunServerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "grpcservice_runserver.RunServerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<servicestubs.runserver.SubmitProgramRequest,
      servicestubs.runserver.SubmitProgramResponse> getSubmitProgramMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "submitProgram",
      requestType = servicestubs.runserver.SubmitProgramRequest.class,
      responseType = servicestubs.runserver.SubmitProgramResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<servicestubs.runserver.SubmitProgramRequest,
      servicestubs.runserver.SubmitProgramResponse> getSubmitProgramMethod() {
    io.grpc.MethodDescriptor<servicestubs.runserver.SubmitProgramRequest, servicestubs.runserver.SubmitProgramResponse> getSubmitProgramMethod;
    if ((getSubmitProgramMethod = RunServerServiceGrpc.getSubmitProgramMethod) == null) {
      synchronized (RunServerServiceGrpc.class) {
        if ((getSubmitProgramMethod = RunServerServiceGrpc.getSubmitProgramMethod) == null) {
          RunServerServiceGrpc.getSubmitProgramMethod = getSubmitProgramMethod =
              io.grpc.MethodDescriptor.<servicestubs.runserver.SubmitProgramRequest, servicestubs.runserver.SubmitProgramResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "submitProgram"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.SubmitProgramRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.SubmitProgramResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RunServerServiceMethodDescriptorSupplier("submitProgram"))
              .build();
        }
      }
    }
    return getSubmitProgramMethod;
  }

  private static volatile io.grpc.MethodDescriptor<servicestubs.runserver.GetStatusRequest,
      servicestubs.runserver.GetStatusResponse> getGetStatusMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getStatus",
      requestType = servicestubs.runserver.GetStatusRequest.class,
      responseType = servicestubs.runserver.GetStatusResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<servicestubs.runserver.GetStatusRequest,
      servicestubs.runserver.GetStatusResponse> getGetStatusMethod() {
    io.grpc.MethodDescriptor<servicestubs.runserver.GetStatusRequest, servicestubs.runserver.GetStatusResponse> getGetStatusMethod;
    if ((getGetStatusMethod = RunServerServiceGrpc.getGetStatusMethod) == null) {
      synchronized (RunServerServiceGrpc.class) {
        if ((getGetStatusMethod = RunServerServiceGrpc.getGetStatusMethod) == null) {
          RunServerServiceGrpc.getGetStatusMethod = getGetStatusMethod =
              io.grpc.MethodDescriptor.<servicestubs.runserver.GetStatusRequest, servicestubs.runserver.GetStatusResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getStatus"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.GetStatusRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.GetStatusResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RunServerServiceMethodDescriptorSupplier("getStatus"))
              .build();
        }
      }
    }
    return getGetStatusMethod;
  }

  private static volatile io.grpc.MethodDescriptor<servicestubs.runserver.GetResultRequest,
      servicestubs.runserver.GetResultResponse> getGetResultMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "getResult",
      requestType = servicestubs.runserver.GetResultRequest.class,
      responseType = servicestubs.runserver.GetResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<servicestubs.runserver.GetResultRequest,
      servicestubs.runserver.GetResultResponse> getGetResultMethod() {
    io.grpc.MethodDescriptor<servicestubs.runserver.GetResultRequest, servicestubs.runserver.GetResultResponse> getGetResultMethod;
    if ((getGetResultMethod = RunServerServiceGrpc.getGetResultMethod) == null) {
      synchronized (RunServerServiceGrpc.class) {
        if ((getGetResultMethod = RunServerServiceGrpc.getGetResultMethod) == null) {
          RunServerServiceGrpc.getGetResultMethod = getGetResultMethod =
              io.grpc.MethodDescriptor.<servicestubs.runserver.GetResultRequest, servicestubs.runserver.GetResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "getResult"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.GetResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  servicestubs.runserver.GetResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new RunServerServiceMethodDescriptorSupplier("getResult"))
              .build();
        }
      }
    }
    return getGetResultMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static RunServerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RunServerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RunServerServiceStub>() {
        @java.lang.Override
        public RunServerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RunServerServiceStub(channel, callOptions);
        }
      };
    return RunServerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static RunServerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RunServerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RunServerServiceBlockingStub>() {
        @java.lang.Override
        public RunServerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RunServerServiceBlockingStub(channel, callOptions);
        }
      };
    return RunServerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static RunServerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<RunServerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<RunServerServiceFutureStub>() {
        @java.lang.Override
        public RunServerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new RunServerServiceFutureStub(channel, callOptions);
        }
      };
    return RunServerServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Contrato c: Client -&gt; RunServer.
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * O cliente envia o programa em blocos e recebe um UUID quando a submissão termina.
     * </pre>
     */
    default io.grpc.stub.StreamObserver<servicestubs.runserver.SubmitProgramRequest> submitProgram(
        io.grpc.stub.StreamObserver<servicestubs.runserver.SubmitProgramResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getSubmitProgramMethod(), responseObserver);
    }

    /**
     */
    default void getStatus(servicestubs.runserver.GetStatusRequest request,
        io.grpc.stub.StreamObserver<servicestubs.runserver.GetStatusResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetStatusMethod(), responseObserver);
    }

    /**
     * <pre>
     * O resultado é devolvido por streaming; podem ser enviados vários blocos.
     * </pre>
     */
    default void getResult(servicestubs.runserver.GetResultRequest request,
        io.grpc.stub.StreamObserver<servicestubs.runserver.GetResultResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetResultMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service RunServerService.
   * <pre>
   * Contrato c: Client -&gt; RunServer.
   * </pre>
   */
  public static abstract class RunServerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return RunServerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service RunServerService.
   * <pre>
   * Contrato c: Client -&gt; RunServer.
   * </pre>
   */
  public static final class RunServerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<RunServerServiceStub> {
    private RunServerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RunServerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RunServerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * O cliente envia o programa em blocos e recebe um UUID quando a submissão termina.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<servicestubs.runserver.SubmitProgramRequest> submitProgram(
        io.grpc.stub.StreamObserver<servicestubs.runserver.SubmitProgramResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getSubmitProgramMethod(), getCallOptions()), responseObserver);
    }

    /**
     */
    public void getStatus(servicestubs.runserver.GetStatusRequest request,
        io.grpc.stub.StreamObserver<servicestubs.runserver.GetStatusResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetStatusMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * O resultado é devolvido por streaming; podem ser enviados vários blocos.
     * </pre>
     */
    public void getResult(servicestubs.runserver.GetResultRequest request,
        io.grpc.stub.StreamObserver<servicestubs.runserver.GetResultResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getGetResultMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service RunServerService.
   * <pre>
   * Contrato c: Client -&gt; RunServer.
   * </pre>
   */
  public static final class RunServerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<RunServerServiceBlockingStub> {
    private RunServerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RunServerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RunServerServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public servicestubs.runserver.GetStatusResponse getStatus(servicestubs.runserver.GetStatusRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetStatusMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * O resultado é devolvido por streaming; podem ser enviados vários blocos.
     * </pre>
     */
    public java.util.Iterator<servicestubs.runserver.GetResultResponse> getResult(
        servicestubs.runserver.GetResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getGetResultMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service RunServerService.
   * <pre>
   * Contrato c: Client -&gt; RunServer.
   * </pre>
   */
  public static final class RunServerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<RunServerServiceFutureStub> {
    private RunServerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected RunServerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new RunServerServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<servicestubs.runserver.GetStatusResponse> getStatus(
        servicestubs.runserver.GetStatusRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetStatusMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_GET_STATUS = 0;
  private static final int METHODID_GET_RESULT = 1;
  private static final int METHODID_SUBMIT_PROGRAM = 2;

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
        case METHODID_GET_STATUS:
          serviceImpl.getStatus((servicestubs.runserver.GetStatusRequest) request,
              (io.grpc.stub.StreamObserver<servicestubs.runserver.GetStatusResponse>) responseObserver);
          break;
        case METHODID_GET_RESULT:
          serviceImpl.getResult((servicestubs.runserver.GetResultRequest) request,
              (io.grpc.stub.StreamObserver<servicestubs.runserver.GetResultResponse>) responseObserver);
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
        case METHODID_SUBMIT_PROGRAM:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.submitProgram(
              (io.grpc.stub.StreamObserver<servicestubs.runserver.SubmitProgramResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSubmitProgramMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              servicestubs.runserver.SubmitProgramRequest,
              servicestubs.runserver.SubmitProgramResponse>(
                service, METHODID_SUBMIT_PROGRAM)))
        .addMethod(
          getGetStatusMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              servicestubs.runserver.GetStatusRequest,
              servicestubs.runserver.GetStatusResponse>(
                service, METHODID_GET_STATUS)))
        .addMethod(
          getGetResultMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              servicestubs.runserver.GetResultRequest,
              servicestubs.runserver.GetResultResponse>(
                service, METHODID_GET_RESULT)))
        .build();
  }

  private static abstract class RunServerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    RunServerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return servicestubs.runserver.RunServerContract.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("RunServerService");
    }
  }

  private static final class RunServerServiceFileDescriptorSupplier
      extends RunServerServiceBaseDescriptorSupplier {
    RunServerServiceFileDescriptorSupplier() {}
  }

  private static final class RunServerServiceMethodDescriptorSupplier
      extends RunServerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    RunServerServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (RunServerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new RunServerServiceFileDescriptorSupplier())
              .addMethod(getSubmitProgramMethod())
              .addMethod(getGetStatusMethod())
              .addMethod(getGetResultMethod())
              .build();
        }
      }
    }
    return result;
  }
}
