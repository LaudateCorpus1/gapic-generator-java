// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.generator.gapic.composer;

import com.google.api.MonitoredResourceDescriptor;
import com.google.api.core.ApiFunction;
import com.google.api.core.ApiFuture;
import com.google.api.core.BetaApi;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.batching.FlowController.LimitExceededBehavior;
import com.google.api.gax.batching.PartitionKey;
import com.google.api.gax.batching.RequestBuilder;
import com.google.api.gax.core.GaxProperties;
import com.google.api.gax.core.GoogleCredentialsProvider;
import com.google.api.gax.core.InstantiatingExecutorProvider;
import com.google.api.gax.grpc.GaxGrpcProperties;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.grpc.ProtoOperationTransformers;
import com.google.api.gax.longrunning.OperationSnapshot;
import com.google.api.gax.longrunning.OperationTimedPollAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.ApiCallContext;
import com.google.api.gax.rpc.ApiClientHeaderProvider;
import com.google.api.gax.rpc.BatchedRequestIssuer;
import com.google.api.gax.rpc.BatchingCallSettings;
import com.google.api.gax.rpc.BatchingDescriptor;
import com.google.api.gax.rpc.ClientContext;
import com.google.api.gax.rpc.OperationCallSettings;
import com.google.api.gax.rpc.PageContext;
import com.google.api.gax.rpc.PagedCallSettings;
import com.google.api.gax.rpc.PagedListDescriptor;
import com.google.api.gax.rpc.PagedListResponseFactory;
import com.google.api.gax.rpc.ServerStreamingCallSettings;
import com.google.api.gax.rpc.StatusCode;
import com.google.api.gax.rpc.StreamingCallSettings;
import com.google.api.gax.rpc.StubSettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.api.gax.rpc.UnaryCallSettings;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.api.generator.engine.ast.AnnotationNode;
import com.google.api.generator.engine.ast.AnonymousClassExpr;
import com.google.api.generator.engine.ast.AssignmentExpr;
import com.google.api.generator.engine.ast.ClassDefinition;
import com.google.api.generator.engine.ast.ConcreteReference;
import com.google.api.generator.engine.ast.Expr;
import com.google.api.generator.engine.ast.ExprStatement;
import com.google.api.generator.engine.ast.IfStatement;
import com.google.api.generator.engine.ast.MethodDefinition;
import com.google.api.generator.engine.ast.MethodInvocationExpr;
import com.google.api.generator.engine.ast.NewObjectExpr;
import com.google.api.generator.engine.ast.NullObjectValue;
import com.google.api.generator.engine.ast.Reference;
import com.google.api.generator.engine.ast.ReferenceConstructorExpr;
import com.google.api.generator.engine.ast.ReturnExpr;
import com.google.api.generator.engine.ast.ScopeNode;
import com.google.api.generator.engine.ast.Statement;
import com.google.api.generator.engine.ast.StringObjectValue;
import com.google.api.generator.engine.ast.TernaryExpr;
import com.google.api.generator.engine.ast.ThisObjectValue;
import com.google.api.generator.engine.ast.ThrowExpr;
import com.google.api.generator.engine.ast.TypeNode;
import com.google.api.generator.engine.ast.ValueExpr;
import com.google.api.generator.engine.ast.VaporReference;
import com.google.api.generator.engine.ast.Variable;
import com.google.api.generator.engine.ast.VariableExpr;
import com.google.api.generator.gapic.model.Field;
import com.google.api.generator.gapic.model.GapicClass;
import com.google.api.generator.gapic.model.Message;
import com.google.api.generator.gapic.model.Method;
import com.google.api.generator.gapic.model.Service;
import com.google.api.generator.gapic.utils.JavaStyle;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.longrunning.Operation;
import com.google.protobuf.Empty;
import io.grpc.serviceconfig.ServiceConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.threeten.bp.Duration;

// TODO(miraleung): Refactor ClassComposer's interface.
public class ServiceStubSettingsClassComposer {
  private static final String CLASS_NAME_PATTERN = "%sStubSettings";
  private static final String GRPC_SERVICE_STUB_PATTERN = "Grpc%sStub";
  private static final String PAGE_STR_DESC_PATTERN = "%s_PAGE_STR_DESC";
  private static final String PAGED_RESPONSE_FACTORY_PATTERN = "%s_PAGE_STR_FACT";
  private static final String PAGED_RESPONSE_TYPE_NAME_PATTERN = "%sPagedResponse";
  private static final String NESTED_BUILDER_CLASS_NAME = "Builder";
  private static final String STUB_PATTERN = "%sStub";

  private static final String LEFT_BRACE = "{";
  private static final String RIGHT_BRACE = "}";
  private static final String SLASH = "/";

  private static final ServiceStubSettingsClassComposer INSTANCE =
      new ServiceStubSettingsClassComposer();

  private static final Map<String, TypeNode> STATIC_TYPES = createStaticTypes();
  private static final VariableExpr DEFAULT_SERVICE_SCOPES_VAR_EXPR =
      createDefaultServiceScopesVarExpr();

  private ServiceStubSettingsClassComposer() {}

  public static ServiceStubSettingsClassComposer instance() {
    return INSTANCE;
  }

  public GapicClass generate(
      Service service, ServiceConfig serviceConfig, Map<String, Message> messageTypes) {
    String pakkage = String.format("%s.stub", service.pakkage());
    Map<String, TypeNode> types = createDynamicTypes(service, pakkage);
    Map<String, VariableExpr> methodSettingsMemberVarExprs =
        createClassMemberVarExprs(service, types);
    String className = getThisClassName(service.name());

    ClassDefinition classDef =
        ClassDefinition.builder()
            .setPackageString(pakkage)
            .setAnnotations(createClassAnnotations())
            .setScope(ScopeNode.PUBLIC)
            .setName(className)
            .setExtendsType(createExtendsType(service, types))
            .setStatements(
                createClassStatements(
                    service, serviceConfig, methodSettingsMemberVarExprs, messageTypes, types))
            .setMethods(createClassMethods(service, methodSettingsMemberVarExprs, types))
            .setNestedClasses(Arrays.asList(createNestedBuilderClass(service, types)))
            .build();
    return GapicClass.create(GapicClass.Kind.STUB, classDef);
  }

  private static List<AnnotationNode> createClassAnnotations() {
    return Arrays.asList(
        AnnotationNode.withType(STATIC_TYPES.get("BetaApi")),
        AnnotationNode.builder()
            .setType(STATIC_TYPES.get("Generated"))
            .setDescription("by gapic-generator-java")
            .build());
  }

  private static TypeNode createExtendsType(Service service, Map<String, TypeNode> types) {
    TypeNode thisClassType = types.get(getThisClassName(service.name()));
    return TypeNode.withReference(
        STATIC_TYPES
            .get("StubSettings")
            .reference()
            .copyAndSetGenerics(Arrays.asList(thisClassType.reference())));
  }

  private static Map<String, VariableExpr> createClassMemberVarExprs(
      Service service, Map<String, TypeNode> types) {
    // Maintain insertion order.
    Map<String, VariableExpr> varExprs = new LinkedHashMap<>();

    // Creates class variables <method>Settings, e.g. echoSettings.
    // TODO(miraleung): Handle batching here.
    for (Method method : service.methods()) {
      TypeNode settingsType = getCallSettingsType(method, types);
      String varName = JavaStyle.toLowerCamelCase(String.format("%sSettings", method.name()));
      varExprs.put(
          varName,
          VariableExpr.withVariable(
              Variable.builder().setType(settingsType).setName(varName).build()));
      if (method.hasLro()) {
        settingsType = getOperationCallSettingsType(method);
        varName = JavaStyle.toLowerCamelCase(String.format("%sOperationSettings", method.name()));
        varExprs.put(
            varName,
            VariableExpr.withVariable(
                Variable.builder().setType(settingsType).setName(varName).build()));
      }
    }

    return varExprs;
  }

  private static List<Statement> createClassStatements(
      Service service,
      ServiceConfig serviceConfig,
      Map<String, VariableExpr> methodSettingsMemberVarExprs,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types) {
    List<Expr> memberVarExprs = new ArrayList<>();

    // Assign DEFAULT_SERVICE_SCOPES.
    VariableExpr defaultServiceScopesDeclVarExpr =
        DEFAULT_SERVICE_SCOPES_VAR_EXPR
            .toBuilder()
            .setIsDecl(true)
            .setScope(ScopeNode.PRIVATE)
            .setIsStatic(true)
            .setIsFinal(true)
            .build();
    MethodInvocationExpr listBuilderExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(STATIC_TYPES.get("ImmutableList"))
            .setGenerics(Arrays.asList(ConcreteReference.withClazz(String.class)))
            .setMethodName("builder")
            .build();
    for (String serviceScope : service.oauthScopes()) {

      listBuilderExpr =
          MethodInvocationExpr.builder()
              .setExprReferenceExpr(listBuilderExpr)
              .setMethodName("add")
              .setArguments(ValueExpr.withValue(StringObjectValue.withValue(serviceScope)))
              .build();
    }
    listBuilderExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(listBuilderExpr)
            .setMethodName("build")
            .setReturnType(DEFAULT_SERVICE_SCOPES_VAR_EXPR.type())
            .build();

    memberVarExprs.add(
        AssignmentExpr.builder()
            .setVariableExpr(defaultServiceScopesDeclVarExpr)
            .setValueExpr(listBuilderExpr)
            .build());

    // Declare settings members.
    memberVarExprs.addAll(
        methodSettingsMemberVarExprs.values().stream()
            .map(
                v ->
                    v.toBuilder()
                        .setIsDecl(true)
                        .setScope(ScopeNode.PRIVATE)
                        .setIsFinal(true)
                        .build())
            .collect(Collectors.toList()));

    memberVarExprs.addAll(
        createPagingStaticAssignExprs(service, serviceConfig, messageTypes, types));
    return memberVarExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList());
  }

  private static List<Expr> createPagingStaticAssignExprs(
      Service service,
      ServiceConfig serviceConfig,
      Map<String, Message> messageTypes,
      Map<String, TypeNode> types) {
    // TODO(miraleung): Add a test case for several such statements.
    List<Expr> descExprs = new ArrayList<>();
    List<Expr> factoryExprs = new ArrayList<>();

    for (Method method : service.methods()) {
      if (!method.isPaged()) {
        continue;
      }
      // Find the repeated type.
      Message pagedResponseMessage =
          messageTypes.get(JavaStyle.toUpperCamelCase(method.outputType().reference().name()));
      TypeNode repeatedResponseType = null;
      for (Field field : pagedResponseMessage.fields()) {
        if (field.isRepeated()) {
          // Field is currently a List-type.
          Preconditions.checkState(
              !field.type().reference().generics().isEmpty(),
              String.format("No generics found for field reference %s", field.type().reference()));
          repeatedResponseType = TypeNode.withReference(field.type().reference().generics().get(0));
        }
      }
      Preconditions.checkNotNull(
          repeatedResponseType,
          String.format(
              "No repeated type found for paged reesponse %s for method %s",
              method.outputType().reference().name(), method.name()));

      // Create the PAGE_STR_DESC variable.
      TypeNode pagedListDescType =
          TypeNode.withReference(
              ConcreteReference.builder()
                  .setClazz(PagedListDescriptor.class)
                  .setGenerics(
                      Arrays.asList(method.inputType(), method.outputType(), repeatedResponseType)
                          .stream()
                          .map(t -> t.reference())
                          .collect(Collectors.toList()))
                  .build());
      String pageStrDescVarName =
          String.format(PAGE_STR_DESC_PATTERN, JavaStyle.toUpperSnakeCase(method.name()));

      VariableExpr pagedListDescVarExpr =
          VariableExpr.withVariable(
              Variable.builder().setType(pagedListDescType).setName(pageStrDescVarName).build());

      descExprs.add(
          createPagedListDescriptorAssignExpr(
              pagedListDescVarExpr, method, repeatedResponseType, types));
      factoryExprs.add(
          createPagedListResponseFactoryAssignExpr(
              pagedListDescVarExpr, method, repeatedResponseType, types));
    }

    descExprs.addAll(factoryExprs);
    return descExprs;
  }

  private static Expr createPagedListDescriptorAssignExpr(
      VariableExpr pagedListDescVarExpr,
      Method method,
      TypeNode repeatedResponseType,
      Map<String, TypeNode> types) {
    MethodDefinition.Builder methodStarterBuilder =
        MethodDefinition.builder().setIsOverride(true).setScope(ScopeNode.PUBLIC);
    List<MethodDefinition> anonClassMethods = new ArrayList<>();

    // Create emptyToken method.
    anonClassMethods.add(
        methodStarterBuilder
            .setReturnType(TypeNode.STRING)
            .setName("emptyToken")
            .setReturnExpr(ValueExpr.withValue(StringObjectValue.withValue("")))
            .build());

    // Create injectToken method.
    VariableExpr payloadVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(method.inputType()).setName("payload").build());
    VariableExpr strTokenVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(TypeNode.STRING).setName("token").build());
    TypeNode returnType = method.inputType();
    Expr newBuilderExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(method.inputType())
            .setMethodName("newBuilder")
            .setArguments(payloadVarExpr)
            .build();
    Expr returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(newBuilderExpr)
            .setMethodName("setPageToken")
            .setArguments(strTokenVarExpr)
            .build();
    returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(returnExpr)
            .setMethodName("build")
            .setReturnType(returnType)
            .build();
    anonClassMethods.add(
        methodStarterBuilder
            .setReturnType(method.inputType())
            .setName("injectToken")
            .setArguments(
                Arrays.asList(payloadVarExpr, strTokenVarExpr).stream()
                    .map(v -> v.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setReturnExpr(returnExpr)
            .build());

    // Create injectPageSize method.
    VariableExpr pageSizeVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(TypeNode.INT).setName("pageSize").build());
    // Re-declare for clarity and easier readeability.
    returnType = method.inputType();
    returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(newBuilderExpr)
            .setMethodName("setPageSize")
            .setArguments(pageSizeVarExpr)
            .build();
    returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(returnExpr)
            .setMethodName("build")
            .setReturnType(returnType)
            .build();
    anonClassMethods.add(
        methodStarterBuilder
            .setReturnType(method.inputType())
            .setName("injectPageSize")
            .setArguments(
                Arrays.asList(payloadVarExpr, pageSizeVarExpr).stream()
                    .map(v -> v.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setReturnExpr(returnExpr)
            .build());

    // TODO(miraleung): Test the edge cases where these proto fields aren't present.
    // Create extractPageSize method.
    returnType = TypeNode.INT_OBJECT;
    anonClassMethods.add(
        methodStarterBuilder
            .setReturnType(returnType)
            .setName("extractPageSize")
            .setArguments(payloadVarExpr.toBuilder().setIsDecl(true).build())
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(payloadVarExpr)
                    .setMethodName("getPageSize")
                    .setReturnType(returnType)
                    .build())
            .build());

    // Create extractNextToken method.
    returnType = TypeNode.STRING;
    payloadVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(method.outputType()).setName("payload").build());
    anonClassMethods.add(
        methodStarterBuilder
            .setReturnType(returnType)
            .setName("extractNextToken")
            .setArguments(payloadVarExpr.toBuilder().setIsDecl(true).build())
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setExprReferenceExpr(payloadVarExpr)
                    .setMethodName("getNextPageToken")
                    .setReturnType(returnType)
                    .build())
            .build());

    // Create extractResources method.
    returnType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(Iterable.class)
                .setGenerics(Arrays.asList(repeatedResponseType.reference()))
                .build());
    Expr getResponsesListExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(payloadVarExpr)
            .setMethodName("getResponsesList")
            .setReturnType(returnType)
            .build();
    Expr conditionExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(
                TypeNode.withReference(ConcreteReference.withClazz(Objects.class)))
            .setMethodName("equals")
            .setArguments(getResponsesListExpr, ValueExpr.withValue(NullObjectValue.create()))
            .setReturnType(TypeNode.BOOLEAN)
            .build();
    Expr thenExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(
                TypeNode.withReference(ConcreteReference.withClazz(ImmutableList.class)))
            .setGenerics(Arrays.asList(repeatedResponseType.reference()))
            .setMethodName("of")
            .setReturnType(returnType)
            .build();

    returnExpr =
        TernaryExpr.builder()
            .setConditionExpr(conditionExpr)
            .setThenExpr(thenExpr)
            .setElseExpr(getResponsesListExpr)
            .build();
    anonClassMethods.add(
        methodStarterBuilder
            .setReturnType(returnType)
            .setName("extractResources")
            .setArguments(payloadVarExpr.toBuilder().setIsDecl(true).build())
            .setReturnExpr(returnExpr)
            .build());

    // Create the anonymous class.
    AnonymousClassExpr pagedListDescAnonClassExpr =
        AnonymousClassExpr.builder()
            .setType(pagedListDescVarExpr.type())
            .setMethods(anonClassMethods)
            .build();

    // Declare and assign the variable.
    return AssignmentExpr.builder()
        .setVariableExpr(
            pagedListDescVarExpr
                .toBuilder()
                .setIsDecl(true)
                .setScope(ScopeNode.PRIVATE)
                .setIsStatic(true)
                .setIsFinal(true)
                .build())
        .setValueExpr(pagedListDescAnonClassExpr)
        .build();
  }

  private static Expr createPagedListResponseFactoryAssignExpr(
      VariableExpr pageStrDescVarExpr,
      Method method,
      TypeNode repeatedResponseType,
      Map<String, TypeNode> types) {
    Preconditions.checkState(
        method.isPaged(), String.format("Method %s is not paged", method.name()));

    // Create the PagedListResponseFactory.
    TypeNode pagedResponseType = types.get(getPagedResponseTypeName(method.name()));
    TypeNode apiFutureType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(ApiFuture.class)
                .setGenerics(Arrays.asList(pagedResponseType.reference()))
                .build());

    VariableExpr callableVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(UnaryCallable.class)
                            .setGenerics(
                                Arrays.asList(
                                    method.inputType().reference(),
                                    method.outputType().reference()))
                            .build()))
                .setName("callable")
                .build());
    VariableExpr requestVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(method.inputType()).setName("request").build());
    VariableExpr contextVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(STATIC_TYPES.get("ApiCallContext"))
                .setName("context")
                .build());
    VariableExpr futureResponseVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(
                    TypeNode.withReference(
                        ConcreteReference.builder()
                            .setClazz(ApiFuture.class)
                            .setGenerics(Arrays.asList(method.outputType().reference()))
                            .build()))
                .setName("futureResponse")
                .build());

    TypeNode pageContextType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(PageContext.class)
                .setGenerics(
                    Arrays.asList(method.inputType(), method.outputType(), repeatedResponseType)
                        .stream()
                        .map(t -> t.reference())
                        .collect(Collectors.toList()))
                .build());
    VariableExpr pageContextVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(pageContextType).setName("pageContext").build());
    AssignmentExpr pageContextAssignExpr =
        AssignmentExpr.builder()
            .setVariableExpr(pageContextVarExpr.toBuilder().setIsDecl(true).build())
            .setValueExpr(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(STATIC_TYPES.get("PageContext"))
                    .setMethodName("create")
                    .setArguments(
                        callableVarExpr, pageStrDescVarExpr, requestVarExpr, contextVarExpr)
                    .setReturnType(pageContextVarExpr.type())
                    .build())
            .build();

    Expr returnExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(types.get(getPagedResponseTypeName(method.name())))
            .setMethodName("createAsync")
            .setArguments(pageContextVarExpr, futureResponseVarExpr)
            .setReturnType(apiFutureType)
            .build();

    MethodDefinition getFuturePagedResponseMethod =
        MethodDefinition.builder()
            .setIsOverride(true)
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(apiFutureType)
            .setName("getFuturePagedResponse")
            .setArguments(
                Arrays.asList(
                        callableVarExpr, requestVarExpr, contextVarExpr, futureResponseVarExpr)
                    .stream()
                    .map(v -> v.toBuilder().setIsDecl(true).build())
                    .collect(Collectors.toList()))
            .setBody(Arrays.asList(ExprStatement.withExpr(pageContextAssignExpr)))
            .setReturnExpr(returnExpr)
            .build();

    // Create the variable.
    TypeNode pagedResponseFactoryType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(PagedListResponseFactory.class)
                .setGenerics(
                    Arrays.asList(
                            method.inputType(),
                            method.outputType(),
                            types.get(getPagedResponseTypeName(method.name())))
                        .stream()
                        .map(t -> t.reference())
                        .collect(Collectors.toList()))
                .build());
    String varName =
        String.format(PAGED_RESPONSE_FACTORY_PATTERN, JavaStyle.toUpperSnakeCase(method.name()));
    VariableExpr pagedListResponseFactoryVarExpr =
        VariableExpr.withVariable(
            Variable.builder().setType(pagedResponseFactoryType).setName(varName).build());
    AnonymousClassExpr factoryAnonClassExpr =
        AnonymousClassExpr.builder()
            .setType(pagedResponseFactoryType)
            .setMethods(Arrays.asList(getFuturePagedResponseMethod))
            .build();

    return AssignmentExpr.builder()
        .setVariableExpr(
            pagedListResponseFactoryVarExpr
                .toBuilder()
                .setIsDecl(true)
                .setScope(ScopeNode.PRIVATE)
                .setIsStatic(true)
                .setIsFinal(true)
                .build())
        .setValueExpr(factoryAnonClassExpr)
        .build();
  }

  private static List<MethodDefinition> createClassMethods(
      Service service,
      Map<String, VariableExpr> methodSettingsMemberVarExprs,
      Map<String, TypeNode> types) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    javaMethods.addAll(createMethodSettingsGetterMethods(methodSettingsMemberVarExprs));
    javaMethods.add(createCreateStubMethod(service, types));
    javaMethods.addAll(createDefaultHelperAndGetterMethods(service, types));
    javaMethods.addAll(createBuilderHelperMethods(service, types));
    javaMethods.add(createClassConstructor(service, methodSettingsMemberVarExprs, types));
    // TODO(miraleung): Fill this out.
    return javaMethods;
  }

  private static List<MethodDefinition> createMethodSettingsGetterMethods(
      Map<String, VariableExpr> methodSettingsMemberVarExprs) {
    Function<Map.Entry<String, VariableExpr>, MethodDefinition> varToMethodFn =
        e ->
            MethodDefinition.builder()
                .setScope(ScopeNode.PUBLIC)
                .setReturnType(e.getValue().type())
                .setName(e.getKey())
                .setReturnExpr(e.getValue())
                .build();
    return methodSettingsMemberVarExprs.entrySet().stream()
        .map(e -> varToMethodFn.apply(e))
        .collect(Collectors.toList());
  }

  private static MethodDefinition createCreateStubMethod(
      Service service, Map<String, TypeNode> types) {
    // Set up the if-statement.
    Expr grpcTransportNameExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(STATIC_TYPES.get("GrpcTransportChannel"))
            .setMethodName("getGrpcTransportName")
            .build();

    Expr getTransportNameExpr =
        MethodInvocationExpr.builder().setMethodName("getTransportChannelProvider").build();
    getTransportNameExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(getTransportNameExpr)
            .setMethodName("getTransportName")
            .build();

    Expr ifConditionExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(getTransportNameExpr)
            .setMethodName("equals")
            .setArguments(grpcTransportNameExpr)
            .setReturnType(TypeNode.BOOLEAN)
            .build();

    Expr createExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(types.get(getGrpcServiceStubTypeName(service.name())))
            .setMethodName("create")
            .setArguments(
                ValueExpr.withValue(
                    ThisObjectValue.withType(types.get(getThisClassName(service.name())))))
            .build();

    IfStatement ifStatement =
        IfStatement.builder()
            .setConditionExpr(ifConditionExpr)
            .setBody(Arrays.asList(ExprStatement.withExpr(ReturnExpr.withExpr(createExpr))))
            .build();

    // Set up exception throwing.
    Expr errorMessageExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(TypeNode.STRING)
            .setMethodName("format")
            .setArguments(
                ValueExpr.withValue(StringObjectValue.withValue("Transport not supported: %s")),
                getTransportNameExpr)
            .setReturnType(TypeNode.STRING)
            .build();
    TypeNode exceptionType = TypeNode.withExceptionClazz(UnsupportedOperationException.class);
    Statement throwStatement =
        ExprStatement.withExpr(
            ThrowExpr.builder().setType(exceptionType).setMessageExpr(errorMessageExpr).build());

    // Put the method together.
    TypeNode returnType = types.get(getServiceStubTypeName(service.name()));
    AnnotationNode annotation =
        AnnotationNode.builder()
            .setType(STATIC_TYPES.get("BetaApi"))
            .setDescription(
                "A restructuring of stub classes is planned, so this may break in the future")
            .build();

    return MethodDefinition.builder()
        .setAnnotations(Arrays.asList(annotation))
        .setScope(ScopeNode.PUBLIC)
        .setReturnType(returnType)
        .setName("createStub")
        .setThrowsExceptions(Arrays.asList(TypeNode.withExceptionClazz(IOException.class)))
        .setBody(Arrays.asList(ifStatement, throwStatement))
        .build();
  }

  private static List<MethodDefinition> createDefaultHelperAndGetterMethods(
      Service service, Map<String, TypeNode> types) {
    List<MethodDefinition> javaMethods = new ArrayList<>();

    // Create the defaultExecutorProviderBuilder method.
    TypeNode returnType =
        TypeNode.withReference(
            ConcreteReference.withClazz(InstantiatingExecutorProvider.Builder.class));
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("defaultExecutorProviderBuilder")
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(STATIC_TYPES.get("InstantiatingExecutorProvider"))
                    .setMethodName("newBuilder")
                    .setReturnType(returnType)
                    .build())
            .build());

    // Create the getDefaultEndpoint method.
    returnType = TypeNode.STRING;
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("getDefaultEndpoint")
            .setReturnExpr(ValueExpr.withValue(StringObjectValue.withValue(service.defaultHost())))
            .build());

    // Create the getDefaultServiceScopes method.
    returnType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(List.class)
                .setGenerics(Arrays.asList(TypeNode.STRING.reference()))
                .build());
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("getDefaultServiceScopes")
            .setReturnExpr(DEFAULT_SERVICE_SCOPES_VAR_EXPR)
            .build());

    // Create the defaultCredentialsProviderBuilder method.
    returnType =
        TypeNode.withReference(
            ConcreteReference.withClazz(GoogleCredentialsProvider.Builder.class));
    MethodInvocationExpr credsProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(STATIC_TYPES.get("GoogleCredentialsProvider"))
            .setMethodName("newBuilder")
            .build();
    credsProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(credsProviderBuilderExpr)
            .setMethodName("setScopesToApply")
            .setArguments(DEFAULT_SERVICE_SCOPES_VAR_EXPR)
            .setReturnType(returnType)
            .build();
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("defaultCredentialsProviderBuilder")
            .setReturnExpr(credsProviderBuilderExpr)
            .build());

    // Create the defaultGrpcTransportProviderBuilder method.
    returnType =
        TypeNode.withReference(
            ConcreteReference.withClazz(InstantiatingGrpcChannelProvider.Builder.class));
    MethodInvocationExpr grpcChannelProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(STATIC_TYPES.get("InstantiatingGrpcChannelProvider"))
            .setMethodName("newBuilder")
            .build();
    grpcChannelProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(grpcChannelProviderBuilderExpr)
            .setMethodName("setMaxInboundMessageSize")
            .setArguments(
                VariableExpr.builder()
                    .setVariable(
                        Variable.builder().setType(TypeNode.INT).setName("MAX_VALUE").build())
                    .setStaticReferenceType(TypeNode.INT_OBJECT)
                    .build())
            .setReturnType(returnType)
            .build();
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("defaultGrpcTransportProviderBuilder")
            .setReturnExpr(grpcChannelProviderBuilderExpr)
            .build());

    // Create the defaultTransportChannelProvider method.
    returnType = STATIC_TYPES.get("TransportChannelProvider");
    MethodInvocationExpr transportProviderBuilderExpr =
        MethodInvocationExpr.builder().setMethodName("defaultGrpcTransportProviderBuilder").build();
    transportProviderBuilderExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(transportProviderBuilderExpr)
            .setMethodName("build")
            .setReturnType(returnType)
            .build();
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("defaultTransportChannelProvider")
            .setReturnExpr(transportProviderBuilderExpr)
            .build());

    // Create the defaultApiClientHeaderProviderBuilder method.
    returnType =
        TypeNode.withReference(ConcreteReference.withClazz(ApiClientHeaderProvider.Builder.class));
    MethodInvocationExpr returnExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(STATIC_TYPES.get("ApiClientHeaderProvider"))
            .setMethodName("newBuilder")
            .build();

    MethodInvocationExpr versionArgExpr =
        MethodInvocationExpr.builder()
            .setStaticReferenceType(STATIC_TYPES.get("GaxProperties"))
            .setMethodName("getLibraryVersion")
            .setArguments(
                VariableExpr.builder()
                    .setVariable(
                        Variable.builder()
                            .setType(
                                TypeNode.withReference(ConcreteReference.withClazz(Class.class)))
                            .setName("class")
                            .build())
                    .setStaticReferenceType(types.get(getThisClassName(service.name())))
                    .build())
            .build();

    returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(returnExpr)
            .setMethodName("setGeneratedLibToken")
            .setArguments(ValueExpr.withValue(StringObjectValue.withValue("gapic")), versionArgExpr)
            .build();
    returnExpr =
        MethodInvocationExpr.builder()
            .setExprReferenceExpr(returnExpr)
            .setMethodName("setTransportToken")
            .setArguments(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(STATIC_TYPES.get("GaxGrpcProperties"))
                    .setMethodName("getGrpcTokenName")
                    .build(),
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(STATIC_TYPES.get("GaxGrpcProperties"))
                    .setMethodName("getGrpcVersion")
                    .build())
            .setReturnType(returnType)
            .build();

    AnnotationNode annotation =
        AnnotationNode.builder()
            .setType(STATIC_TYPES.get("BetaApi"))
            .setDescription(
                "The surface for customizing headers is not stable yet and may change in the"
                    + " future.")
            .build();
    javaMethods.add(
        MethodDefinition.builder()
            .setAnnotations(Arrays.asList(annotation))
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(returnType)
            .setName("defaultApiClientHeaderProviderBuilder")
            .setReturnExpr(returnExpr)
            .build());

    return javaMethods;
  }

  private static List<MethodDefinition> createBuilderHelperMethods(
      Service service, Map<String, TypeNode> types) {
    List<MethodDefinition> javaMethods = new ArrayList<>();
    // Create the newBuilder() method.
    final TypeNode builderReturnType = types.get(NESTED_BUILDER_CLASS_NAME);
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(builderReturnType)
            .setName("newBuilder")
            .setReturnExpr(
                MethodInvocationExpr.builder()
                    .setStaticReferenceType(builderReturnType)
                    .setMethodName("createDefault")
                    .setReturnType(builderReturnType)
                    .build())
            .build());

    // Create the newBuilder(ClientContext) method.
    Function<Expr, NewObjectExpr> newBuilderFn =
        argExpr -> NewObjectExpr.builder().setType(builderReturnType).setArguments(argExpr).build();
    VariableExpr clientContextVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(STATIC_TYPES.get("ClientContext"))
                .setName("clientContext")
                .build());
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setIsStatic(true)
            .setReturnType(builderReturnType)
            .setName("newBuilder")
            .setArguments(clientContextVarExpr.toBuilder().setIsDecl(true).build())
            .setReturnExpr(newBuilderFn.apply(clientContextVarExpr))
            .build());

    // Create the toBuilder method.
    javaMethods.add(
        MethodDefinition.builder()
            .setScope(ScopeNode.PUBLIC)
            .setReturnType(builderReturnType)
            .setName("toBuilder")
            .setReturnExpr(
                newBuilderFn.apply(
                    ValueExpr.withValue(
                        ThisObjectValue.withType(types.get(getThisClassName(service.name()))))))
            .build());

    return javaMethods;
  }

  private static MethodDefinition createClassConstructor(
      Service service,
      Map<String, VariableExpr> methodSettingsMemberVarExprs,
      Map<String, TypeNode> types) {
    TypeNode thisType = types.get(getThisClassName(service.name()));
    final VariableExpr settingsBuilderVarExpr =
        VariableExpr.withVariable(
            Variable.builder()
                .setType(types.get(NESTED_BUILDER_CLASS_NAME))
                .setName("settingsBuilder")
                .build());

    Expr superCtorExpr =
        ReferenceConstructorExpr.superBuilder()
            .setType(STATIC_TYPES.get("StubSettings"))
            .setArguments(settingsBuilderVarExpr)
            .build();

    List<Expr> bodyExprs = new ArrayList<>();
    bodyExprs.add(superCtorExpr);

    Function<Map.Entry<String, VariableExpr>, AssignmentExpr> varInitExprFn =
        e ->
            AssignmentExpr.builder()
                .setVariableExpr(e.getValue())
                .setValueExpr(
                    MethodInvocationExpr.builder()
                        .setExprReferenceExpr(
                            MethodInvocationExpr.builder()
                                .setExprReferenceExpr(settingsBuilderVarExpr)
                                .setMethodName(e.getKey())
                                .build())
                        .setMethodName("build")
                        .setReturnType(e.getValue().type())
                        .build())
                .build();
    bodyExprs.addAll(
        methodSettingsMemberVarExprs.entrySet().stream()
            .map(e -> varInitExprFn.apply(e))
            .collect(Collectors.toList()));

    return MethodDefinition.constructorBuilder()
        .setScope(ScopeNode.PROTECTED)
        .setReturnType(thisType)
        .setArguments(settingsBuilderVarExpr.toBuilder().setIsDecl(true).build())
        .setThrowsExceptions(Arrays.asList(TypeNode.withExceptionClazz(IOException.class)))
        .setBody(
            bodyExprs.stream().map(e -> ExprStatement.withExpr(e)).collect(Collectors.toList()))
        .build();
  }

  private static ClassDefinition createNestedBuilderClass(
      Service service, Map<String, TypeNode> types) {
    String thisClassName = getThisClassName(service.name());
    TypeNode outerThisClassType = types.get(thisClassName);

    String className = "Builder";

    // TODO(miraleung): Fill this out.
    return ClassDefinition.builder()
        .setIsNested(true)
        .setScope(ScopeNode.PUBLIC)
        .setIsStatic(true)
        .setName(className)
        .build();
  }

  private static Map<String, TypeNode> createStaticTypes() {
    List<Class> concreteClazzes =
        Arrays.asList(
            ApiCallContext.class,
            ApiClientHeaderProvider.class,
            ApiFunction.class,
            ApiFuture.class,
            BatchedRequestIssuer.class,
            BatchingCallSettings.class,
            BatchingDescriptor.class,
            BatchingSettings.class,
            BetaApi.class,
            ClientContext.class,
            Duration.class,
            Empty.class,
            FlowControlSettings.class,
            GaxGrpcProperties.class,
            GaxProperties.class,
            Generated.class,
            GoogleCredentialsProvider.class,
            GrpcTransportChannel.class,
            IOException.class,
            ImmutableList.class,
            ImmutableMap.class,
            ImmutableSet.class,
            InstantiatingExecutorProvider.class,
            InstantiatingGrpcChannelProvider.class,
            LimitExceededBehavior.class,
            List.class,
            Lists.class,
            MonitoredResourceDescriptor.class,
            Operation.class,
            OperationCallSettings.class,
            OperationSnapshot.class,
            OperationTimedPollAlgorithm.class,
            PageContext.class,
            PagedCallSettings.class,
            PagedListDescriptor.class,
            PagedListResponseFactory.class,
            PartitionKey.class,
            ProtoOperationTransformers.class,
            RequestBuilder.class,
            RetrySettings.class,
            ServerStreamingCallSettings.class,
            StatusCode.class,
            StreamingCallSettings.class,
            StubSettings.class,
            TransportChannelProvider.class,
            UnaryCallSettings.class,
            UnaryCallable.class);
    return concreteClazzes.stream()
        .collect(
            Collectors.toMap(
                c -> c.getSimpleName(),
                c -> TypeNode.withReference(ConcreteReference.withClazz(c))));
  }

  private static Map<String, TypeNode> createDynamicTypes(Service service, String pakkage) {
    String thisClassName = getThisClassName(service.name());
    Map<String, TypeNode> dynamicTypes = new HashMap<>();

    // This type.
    dynamicTypes.put(
        thisClassName,
        TypeNode.withReference(
            VaporReference.builder().setName(thisClassName).setPakkage(pakkage).build()));

    // Nested builder class.
    dynamicTypes.put(
        NESTED_BUILDER_CLASS_NAME,
        TypeNode.withReference(
            VaporReference.builder()
                .setName(NESTED_BUILDER_CLASS_NAME)
                .setPakkage(pakkage)
                .setEnclosingClassName(thisClassName)
                .setIsStaticImport(true)
                .build()));

    // Other generated stub classes.
    dynamicTypes.putAll(
        Arrays.asList(GRPC_SERVICE_STUB_PATTERN, STUB_PATTERN).stream()
            .collect(
                Collectors.toMap(
                    p -> String.format(p, service.name()),
                    p ->
                        TypeNode.withReference(
                            VaporReference.builder()
                                .setName(String.format(p, service.name()))
                                .setPakkage(pakkage)
                                .build()))));

    // Pagination types.
    dynamicTypes.putAll(
        service.methods().stream()
            .filter(m -> m.isPaged())
            .collect(
                Collectors.toMap(
                    m -> String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, m.name()),
                    m ->
                        TypeNode.withReference(
                            VaporReference.builder()
                                .setName(getPagedResponseTypeName(m.name()))
                                .setPakkage(service.pakkage())
                                .setEnclosingClassName(String.format("%sClient", service.name()))
                                .setIsStaticImport(true)
                                .build()))));

    // TODO(miraleung): Fill this out.
    return dynamicTypes;
  }

  private static VariableExpr createDefaultServiceScopesVarExpr() {
    TypeNode listStringType =
        TypeNode.withReference(
            ConcreteReference.builder()
                .setClazz(ImmutableList.class)
                .setGenerics(Arrays.asList(ConcreteReference.withClazz(String.class)))
                .build());
    return VariableExpr.withVariable(
        Variable.builder().setName("DEFAULT_SERVICE_SCOPES").setType(listStringType).build());
  }

  private static String getThisClassName(String serviceName) {
    return String.format(CLASS_NAME_PATTERN, JavaStyle.toUpperCamelCase(serviceName));
  }

  private static String getPagedResponseTypeName(String methodName) {
    return String.format(PAGED_RESPONSE_TYPE_NAME_PATTERN, JavaStyle.toUpperCamelCase(methodName));
  }

  private static String getServiceStubTypeName(String serviceName) {
    return String.format(STUB_PATTERN, JavaStyle.toUpperCamelCase(serviceName));
  }

  private static String getGrpcServiceStubTypeName(String serviceName) {
    return String.format(GRPC_SERVICE_STUB_PATTERN, JavaStyle.toUpperCamelCase(serviceName));
  }

  private static TypeNode getCallSettingsType(Method method, Map<String, TypeNode> types) {
    // Default: No streaming.
    TypeNode callSettingsType =
        method.isPaged()
            ? STATIC_TYPES.get("PagedCallSettings")
            : STATIC_TYPES.get("UnaryCallSettings");

    // Streaming takes precendence over paging, as per the monolith's existing behavior.
    switch (method.stream()) {
      case SERVER:
        callSettingsType = STATIC_TYPES.get("ServerStreamingCallSettings");
        break;
      case CLIENT:
        // Fall through.
      case BIDI:
        callSettingsType = STATIC_TYPES.get("StreamingCallSettings");
        break;
      case NONE:
        // Fall through.
      default:
        break;
    }

    List<Reference> generics = new ArrayList<>();
    generics.add(method.inputType().reference());
    generics.add(method.outputType().reference());
    if (method.isPaged()) {
      generics.add(types.get(getPagedResponseTypeName(method.name())).reference());
    }
    return TypeNode.withReference(callSettingsType.reference().copyAndSetGenerics(generics));
  }

  private static TypeNode getOperationCallSettingsType(Method method) {
    Preconditions.checkState(
        method.hasLro(),
        String.format("Cannot get OperationCallSettings for non-LRO method %s", method.name()));
    List<Reference> generics = new ArrayList<>();
    generics.add(method.inputType().reference());
    generics.add(method.lro().responseType().reference());
    generics.add(method.lro().metadataType().reference());
    return TypeNode.withReference(
        STATIC_TYPES.get("OperationCallSettings").reference().copyAndSetGenerics(generics));
  }
}