#include <jni.h>
#include <dirent.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define FDINFO_PATH "/proc/self/fdinfo"
#define MAX_PATH_LEN 128
#define LINE_BUF_LEN 256

/*
 * Scans one /proc/self/fdinfo/<fd> file for drm-engine-* busy-time values.
 * Accumulates nanosecond counts into the four output pointers.
 * Lines of interest:
 *   drm-engine-render: / drm-engine-fragment: / drm-engine-gfx: → render
 *   drm-engine-vertex-tiler: / drm-engine-tiler:                  → tiler
 *   drm-engine-compute:                                            → compute
 */
static void scan_fdinfo(const char *path,
                        long long *render_ns,
                        long long *tiler_ns,
                        long long *compute_ns) {
    FILE *f = fopen(path, "r");
    if (!f) return;

    char line[LINE_BUF_LEN];
    while (fgets(line, sizeof(line), f)) {
        int is_render  = (strncmp(line, "drm-engine-render:",   18) == 0 ||
                          strncmp(line, "drm-engine-fragment:", 20) == 0 ||
                          strncmp(line, "drm-engine-gfx:",      15) == 0);
        int is_tiler   = (strncmp(line, "drm-engine-vertex-tiler:", 24) == 0 ||
                          strncmp(line, "drm-engine-tiler:",        17) == 0);
        int is_compute = (strncmp(line, "drm-engine-compute:", 19) == 0);

        if (is_render || is_tiler || is_compute) {
            char *colon = strchr(line, ':');
            if (!colon) continue;
            char *p = colon + 1;
            while (*p == ' ' || *p == '\t') ++p;
            if (*p < '0' || *p > '9') continue;
            long long val = strtoll(p, NULL, 10);
            if (val < 0) continue;
            if (is_render)  *render_ns  += val;
            if (is_tiler)   *tiler_ns   += val;
            if (is_compute) *compute_ns += val;
        }
    }
    fclose(f);
}

/*
 * Reads /proc/self/fdinfo/* and returns cumulative DRM engine busy-time.
 * Returns a jdoubleArray[4]: {renderNs, tilerNs, computeNs, totalNs}.
 * Returns NULL if the directory cannot be opened.
 */
JNIEXPORT jdoubleArray JNICALL
Java_com_smellouk_kamper_gpu_repository_source_FdinfoJni_readEngineNs(
        JNIEnv *env, jclass clazz) {
    long long render_ns = 0, tiler_ns = 0, compute_ns = 0;

    DIR *dir = opendir(FDINFO_PATH);
    if (!dir) return NULL;

    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
        if (ent->d_name[0] == '.') continue;
        char path[MAX_PATH_LEN];
        int written = snprintf(path, sizeof(path), "%s/%s", FDINFO_PATH, ent->d_name);
        if (written <= 0 || written >= (int)sizeof(path)) continue;
        scan_fdinfo(path, &render_ns, &tiler_ns, &compute_ns);
    }
    closedir(dir);

    long long total_ns = render_ns + tiler_ns + compute_ns;

    jdoubleArray result = (*env)->NewDoubleArray(env, 4);
    if (!result) return NULL;

    double vals[4] = {
        (double)render_ns,
        (double)tiler_ns,
        (double)compute_ns,
        (double)total_ns
    };
    (*env)->SetDoubleArrayRegion(env, result, 0, 4, vals);
    return result;
}
