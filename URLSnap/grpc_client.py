import grpc
import url_pb2
import url_pb2_grpc

def main():
    channel = grpc.insecure_channel('localhost:9090')
    stub = url_pb2_grpc.UrlServiceStub(channel)

    username = "admin"  # Asegúrate que este usuario existe

    # (a) Listar URLs
    print("📥 Obteniendo URLs del usuario...")
    request = url_pb2.UrlRequest(username=username)

    try:
        response = stub.ListUserUrls(request)
        print("✅ URLs recibidas:")
        for url in response.urls:
            print(f"- {url.originalUrl} → {url.shortUrl}")
            print(f"  📅 Creado en: {url.createdAt}")
            print(f"  🖼 Vista previa (base64): {'✅' if url.previewBase64 else '❌'}")
            print(f"  📊 Stats:")
            for stat in url.stats:
                print(f"    - {stat.accessedAt} | IP: {stat.ipAddress} | Navegador: {stat.browser} | SO: {stat.os}")
    except grpc.RpcError as e:
        print(f"❌ Error en la solicitud ListUserUrls: {e.details()}")

    # (b) Crear nueva URL
    print("\n🚀 Creando nueva URL para el usuario...")
    create_request = url_pb2.CreateUrlRequest(
        username=username,
        originalUrl="https://chat.openai.com/"  # Puedes cambiarla para probar con otras
    )

    try:
        create_response = stub.CreateUrl(create_request)
        url = create_response.url
        print("✅ URL creada o existente:")
        print(f"- Original: {url.originalUrl}")
        print(f"- Cortada: {url.shortUrl}")
        print(f"- Fecha creación: {url.createdAt}")
        print(f"- Vista previa (base64): {'✅' if url.previewBase64 else '❌'}")
        print(f"- Stats actuales: {len(url.stats)}")
    except grpc.RpcError as e:
        print(f"❌ Error en la solicitud CreateUrl: {e.details()}")

if __name__ == "__main__":
    main()
