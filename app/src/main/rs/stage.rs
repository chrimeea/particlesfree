#pragma version(1)
#pragma rs java_package_name(com.prozium.particlesfree)
#pragma rs_fp_relaxed

#define MASS 1.0
#define index_2(A, B) ((A) * 2 + (B))
#define attraction0(MA, MB, D, H) (((MB) / (MA)) / fmax(D, H))
#define attraction1(MA, MB, D, H) (((MB) / (MA)) * fmax(D, H))
float width, height, radius, *grid_mass, attenuation, s_gravity[4], s_force[4], s_horizon[4], s_gauss[4], scale, *extra, mforce, *factor, prev_factor;
int32_t s_formula[4], *pgrid, *hole, *ptype, *grid_type, total_grid, total_hole, bounce, total, *axis, *axis_index;
char *hit;
float2 *forces, *grid_center, *grid_forces, *pos, *instant, pinch;
float4 *quads;

static float attraction(const float ma, const float mb, const float d, const int32_t t1, const int32_t t2) {
    if (s_formula[index_2(t1, t2)] == 0) {
        if (d < s_gauss[index_2(t1, t2)]) {
            return -attraction0(ma, mb, d, s_horizon[index_2(t1, t2)]);
        } else {
            return attraction0(ma, mb, d, s_horizon[index_2(t1, t2)]);
        }
    } else {
        if (d < s_gauss[index_2(t1, t2)]) {
            return -attraction0(ma, mb, d, s_horizon[index_2(t1, t2)]);
        } else if (d < 2.0 * s_gauss[index_2(t1, t2)]) {
            return attraction1(ma, mb, d, s_horizon[index_2(t1, t2)]);
        } else {
            return 0.0;
        }
    }
}

void root1(const float2 *v_in, float2 *v_out, const void *usrData, uint32_t x) {
    int32_t i;
    float d;
    float2 f[2];
    if (x < total_grid && grid_mass[x] > 0.0) {
        f[0] = f[1] = 0.0;
        for (i = total_grid - 1; i >= 0 ; i--) {
            if (grid_mass[i] > 0.0 && i != x) {
                d = distance(grid_center[i], grid_center[x]) * s_gravity[index_2(grid_type[x], grid_type[i])];
                f[grid_type[i]] += attraction(MASS, grid_mass[i], d * d, grid_type[x], grid_type[i]) * normalize(grid_center[i] - grid_center[x]);
            }
        }
        grid_forces[x] = f[0] * s_force[index_2(grid_type[x], 0)] + f[1] * s_force[index_2(grid_type[x], 1)] + pinch;
    }
}

void root2(const float2 *v_in, float2 *v_out, const void *usrData, uint32_t x) {
    float d = distance(grid_center[pgrid[x] - 1], *v_in) * s_gravity[index_2(ptype[x], ptype[x])];
    float a = attraction(MASS, grid_mass[pgrid[x] - 1] - 1.0, d * d, ptype[x], ptype[x]) * s_force[index_2(ptype[x], ptype[x])];
    forces[x] *= attenuation;
    if (d > 0.0) {
        forces[x] += grid_forces[pgrid[x] - 1] + a * normalize(grid_center[pgrid[x] - 1] - *v_in);
    } else if (a > 0.0) {
        a /= length(grid_forces[pgrid[x] - 1]);
        if (a < 1.0) {
            forces[x] += grid_forces[pgrid[x] - 1] * (1.0 - a);
        }
    } else {
        forces[x] += grid_forces[pgrid[x] - 1];
    }
    *v_out = *v_in;
    if (fabs(v_out->x + forces[x].x) > width) {
        forces[x].x = -forces[x].x;
        if (fabs(v_out->y + forces[x].y) > height) {
            forces[x] = 0.0;
        } else if (bounce == 0) {
            forces[x] = normalize(forces[x]) * fabs(forces[x].y);
        }
    } else if (fabs(v_out->y + forces[x].y) > height) {
        forces[x].y = -forces[x].y;
        if (bounce == 0) {
            forces[x] = normalize(forces[x]) * fabs(forces[x].x);
        }
    }
    *v_out += forces[x];
    quads[6 * x] = (float4){v_out->x, v_out->y, 0.0, 1.0} + (float4){-1.0, 1.0, 0.0, 0.0} * scale;
    quads[6 * x + 1] = (float4){v_out->x, v_out->y, 0.0, 0.0} + (float4){-1.0, -1.0, 0.0, 0.0} * scale;
    quads[6 * x + 2] = (float4){v_out->x, v_out->y, 1.0, 0.0} + (float4){1.0, -1.0, 0.0, 0.0} * scale;
    quads[6 * x + 3] = (float4){v_out->x, v_out->y, 0.0, 1.0} + (float4){-1.0, 1.0, 0.0, 0.0} * scale;
    quads[6 * x + 4] = (float4){v_out->x, v_out->y, 1.0, 0.0} + (float4){1.0, -1.0, 0.0, 0.0} * scale;
    quads[6 * x + 5] = (float4){v_out->x, v_out->y, 1.0, 1.0} + (float4){1.0, 1.0, 0.0, 0.0} * scale;
    extra[6 * x] = length(forces[x]);
    extra[6 * x + 1] = extra[6 * x];
    extra[6 * x + 2] = extra[6 * x];
    extra[6 * x + 3] = extra[6 * x];
    extra[6 * x + 4] = extra[6 * x];
    extra[6 * x + 5] = extra[6 * x];
}

void func1(rs_allocation position) {
    float2 pi;
    int32_t j, i = rsAllocationGetDimX(position);
    while (i > 0) {
        pi = rsGetElementAt_float2(position, --i);
        if (pgrid[i] == 0 || distance(pi.xy, grid_center[pgrid[i] - 1]) > radius) {
            if (pgrid[i] > 0) {
                grid_mass[pgrid[i] - 1] -= MASS;
                if (fabs(grid_mass[pgrid[i] - 1]) < 0.1) {
                    grid_mass[pgrid[i] - 1] = 0.0;
                    if (pgrid[i] == total_grid) {
                        total_grid--;
                    } else {
                        hole[total_hole++] = pgrid[i];
                    }
                }
            }
            for (j = total_grid - 1; j >= 0; j--) {
                if (grid_type[j] == ptype[i] && grid_mass[j] > 0.0 && distance(pi, grid_center[j]) <= radius) {
                    grid_mass[j] += MASS;
                    pgrid[i] = j + 1;
                    break;
                }
            }
            if (j < 0) {
                if (total_hole > 0) {
                    pgrid[i] = hole[--total_hole];
                } else {
                    pgrid[i] = ++total_grid;
                }
                grid_center[pgrid[i] - 1] = pi;
                grid_mass[pgrid[i] - 1] = MASS;
                grid_type[pgrid[i] - 1] = ptype[i];
            }
        }
    }
}

#define MAIN_AXIS (width > height ? 0 : 1)
#define travel_towards(PA, PB, FA, FB) ((PB.x - PA.x) * (FB.x - FA.x) + (PB.y - PA.y) * (FB.y - FA.y) < 0.0)

static void sort(const int32_t i, const int32_t j) {
    int32_t k;
    if (axis_index[i] > 0 && pos[i][j] < pos[axis[axis_index[i] - 1]][j])  {
        k = axis_index[i];
        while (k > 0 && pos[i][j] < pos[axis[k - 1]][j]) {
            axis_index[axis[k - 1]]++;
            axis[k] = axis[k - 1];
            k--;
        }
        axis[k] = i;
        axis_index[i] = k;
    } else if (axis_index[i] + 1 < total && pos[i][j] > pos[axis[axis_index[i] + 1]][j]) {
        k = axis_index[i];
        while (k + 1 < total && pos[i][j] > pos[axis[k + 1]][j]) {
            axis_index[axis[k + 1]]--;
            axis[k] = axis[k + 1];
            k++;
        }
        axis[k] = i;
        axis_index[i] = k;
    }
}

void func2(const int32_t t) {
    int32_t i;
    prev_factor = *factor;
    total = 0;
    mforce = 0.0;
    for (i = 0; i < t; i++) {
        pos[i] = (float2) {rsRand(-width, width), rsRand(-height, height)};
        axis_index[i] = axis[i] = i;
        total++;
        sort(i, MAIN_AXIS);
    }
}

void func3(rs_allocation next_forces) {
    int32_t x;
    mforce = 0.0;
    for (x = 0; x < total; x++) {
        //pos[x] += (hit[x] == 0 ? rsGetElementAt_float2(next_forces, x) : instant[x]);
        pos[x] += rsGetElementAt_float2(next_forces, x) + instant[x];
        if (pos[x].x > width) {
            pos[x].x = width;
        } else if (pos[x].x < -width) {
            pos[x].x = -width;
        }
        if (pos[x].y > height) {
            pos[x].y = height;
        } else if (pos[x].y < -height) {
            pos[x].y = -height;
        }
        sort(x, MAIN_AXIS);
        forces[x] = rsGetElementAt_float2(next_forces, x) * attenuation * (*factor / prev_factor);
        if (hit[x]) {
            hit[x] = 0;
        } else {
            forces[x] += pinch;
        }
        mforce = fmax(mforce, length(forces[x]));
        quads[6 * x] = (float4) {pos[x].x, pos[x].y, 0.0, 1.0} + (float4) {-1.0, 1.0, 0.0, 0.0} * scale;
        quads[6 * x + 1] = (float4) {pos[x].x, pos[x].y, 0.0, 0.0} + (float4) {-1.0, -1.0, 0.0, 0.0} * scale;
        quads[6 * x + 2] = (float4) {pos[x].x, pos[x].y, 1.0, 0.0} + (float4) {1.0, -1.0, 0.0, 0.0} * scale;
        quads[6 * x + 3] = (float4) {pos[x].x, pos[x].y, 0.0, 1.0} + (float4) {-1.0, 1.0, 0.0, 0.0} * scale;
        quads[6 * x + 4] = (float4) {pos[x].x, pos[x].y, 1.0, 0.0} + (float4) {1.0, -1.0, 0.0, 0.0} * scale;
        quads[6 * x + 5] = (float4) {pos[x].x, pos[x].y, 1.0, 1.0} + (float4) {1.0, 1.0, 0.0, 0.0} * scale;
        extra[6 * x] = length(forces[x]);
        extra[6 * x + 1] = extra[6 * x];
        extra[6 * x + 2] = extra[6 * x];
        extra[6 * x + 3] = extra[6 * x];
        extra[6 * x + 4] = extra[6 * x];
        extra[6 * x + 5] = extra[6 * x];
    }
    prev_factor = *factor;
    *factor = fmin((float) 1.0, *factor * scale / mforce);
}

void root5(const float2 *v_in, float2 *v_out, const void *usrData, uint32_t x) {
    int32_t i, j = MAIN_AXIS, k;
    float d, t = 0.0;
    float2 n, c = 0.0, f = 0.0;
    i = axis_index[x] + 1;
    instant[x] = 0.0;
    while (i < total && pos[x][j] + scale / 2.0 > pos[axis[i]][j] - scale / 2.0) {
        k = axis[i];
        d = distance(pos[x], pos[k]);
        if (d < scale && d > 0.0 && travel_towards(pos[x], pos[k], forces[x], forces[k])) {
            c += pos[k];
            f += forces[k];
            t++;
            instant[x] += (pos[x] - pos[k]) * (scale - d) / (2.0 * d);
        }
        i++;
    }
    i = axis_index[x] - 1;
    while (i >= 0 && pos[x][j] - scale / 2.0 < pos[axis[i]][j] + scale / 2.0) {
        k = axis[i];
        d = distance(pos[x], pos[k]);
        if (d < scale && d > 0.0 && travel_towards(pos[x], pos[k], forces[x], forces[k])) {
            c += pos[k];
            f += forces[k];
            t++;
            instant[x] += (pos[x] - pos[k]) * (scale - d) / (2.0 * d);
        }
        i--;
    }
    if (fabs(pos[x].x) > width - scale / 2.0 && fabs(pos[x].x + forces[x].x) > fabs(pos[x].x)) {
        n.x = forces[x].x > 0.0 ? width + scale / 2.0 : -width - scale / 2.0;
        if (fabs(pos[x].y) > height - scale / 2.0 && fabs(pos[x].y + forces[x].y) > fabs(pos[x].y)) {
            n.y = forces[x].y > 0.0 ? height + scale / 2.0 : -height - scale / 2.0;
        } else {
            n.y = pos[x].y;
        }
        c += n;
        d = distance(pos[x], n);
        t++;
        instant[x] += (pos[x] - n) * (scale - d) / (2.0 * d);
    } else if (fabs(pos[x].y) > height - scale / 2.0 && fabs(pos[x].y + forces[x].y) > fabs(pos[x].y)) {
        n.x = pos[x].x;
        n.y = forces[x].y > 0.0 ? height + scale / 2.0 : -height - scale / 2.0;
        c += n;
        d = distance(pos[x], n);
        t++;
        instant[x] += (pos[x] - n) * (scale - d) / (2.0 * d);
    }
    if (t > 0.0) {
        instant[x] /= t;
        c /= t;
        f /= t;
        n = normalize(c - pos[x]);
        *v_out = forces[x] - (2.0 * (forces[x].x * n.x + forces[x].y * n.y - f.x * n.x - f.y * n.y) / (MASS + MASS)) * MASS * n;
        hit[x] = 1;
    } else {
        *v_out = forces[x];
    }
}
