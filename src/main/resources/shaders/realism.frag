#version 330 core

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

out vec4 FragColor;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform bool useBlinnPhong;
uniform bool flatShading;
uniform bool useTexture;
uniform bool shadowsEnabled;
uniform sampler2D surfaceTexture;
uniform sampler2D shadowMap;

float calculateShadow(vec3 normal, vec3 lightDir) {
    vec3 projected = FragPosLightSpace.xyz / FragPosLightSpace.w;
    projected = projected * 0.5 + 0.5;
    if (projected.z > 1.0 || any(lessThan(projected.xy, vec2(0.0)))
            || any(greaterThan(projected.xy, vec2(1.0)))) {
        return 0.0;
    }

    float bias = max(0.003 * (1.0 - dot(normal, lightDir)), 0.0005);
    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closestDepth = texture(shadowMap,
                    projected.xy + vec2(x, y) * texelSize).r;
            shadow += projected.z - bias > closestDepth ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    vec3 norm = flatShading
            ? normalize(cross(dFdx(FragPos), dFdy(FragPos)))
            : normalize(Normal);
    if (!gl_FrontFacing) norm = -norm;

    vec3 lightDir = normalize(lightPos - FragPos);
    vec3 viewDir = normalize(viewPos - FragPos);
    float diffuseFactor = max(dot(norm, lightDir), 0.0);

    float specularFactor;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        specularFactor = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        specularFactor = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    }

    vec3 baseColor = useTexture
            ? texture(surfaceTexture, TexCoords).rgb
            : objectColor;
    float shadow = shadowsEnabled ? calculateShadow(norm, lightDir) : 0.0;
    vec3 ambient = 0.22 * lightColor * baseColor;
    vec3 diffuse = (1.0 - shadow) * diffuseFactor * lightColor * baseColor;
    vec3 specular = (1.0 - shadow) * 0.65 * specularFactor * lightColor;
    FragColor = vec4(ambient + diffuse + specular, 1.0);
}
