package com.vauth.foxyvpn.ui.l10n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.vauth.foxyvpn.ui.theme.AppLanguage

/**
 * Maintainable UI localization. English is the source of truth; Persian
 * translations live in [FA]. Untranslated keys fall back to English.
 * Emails, server names, IPs and numeric values are never passed through
 * this map — callers keep them as raw data.
 */
object L10n {
    private val FA: Map<String, String> = mapOf(
        "  Sign out" to "  خروج",
        "AMOLED black" to "مشکی AMOLED",
        "About" to "درباره",
        "Accent color" to "رنگ تأکید",
        "Account" to "حساب",
        "Accounts" to "حساب‌ها",
        "Add account" to "افزودن حساب",
        "Advanced" to "پیشرفته",
        "All" to "همه",
        "All traffic is protected" to "همه ترافیک محافظت می‌شود",
        "Apply" to "اعمال",
        "Ask before deleting an account" to "قبل از حذف حساب بپرس",
        "Automatically chosen for the best performance" to "به‌صورت خودکار برای بهترین عملکرد انتخاب می‌شود",
        "Back" to "بازگشت",
        "Back home after selecting" to "بازگشت به خانه پس از انتخاب",
        "Batch import" to "ورود دسته‌ای",
        "Batch import accounts" to "ورود دسته‌ای حساب‌ها",
        "Behaviour" to "رفتار",
        "Best performance" to "بهترین عملکرد",
        "Block ads" to "مسدودسازی تبلیغات",
        "Cancel" to "لغو",
        "Card corners" to "گوشه‌های کارت",
        "Chain through upstream proxy" to "زنجیره از طریق پروکسی بالادستی",
        "Check that the tunnel exits in the selected country" to "بررسی کنید تونل در کشور انتخاب‌شده خارج شود",
        "Choose location" to "انتخاب مکان",
        "Clear logs" to "پاک کردن گزارش‌ها",
        "Close" to "بستن",
        "Color style" to "سبک رنگ",
        "Colors" to "رنگ‌ها",
        "Comfortable" to "راحت",
        "Compact" to "فشرده",
        "Confirm before removing" to "قبل از حذف تأیید کن",
        "Confirmation code" to "کد تأیید",
        "Connect" to "اتصال",
        "Connect button style" to "سبک دکمه اتصال",
        "Connect to the VPN server through another proxy first" to "ابتدا از طریق پروکسی دیگری به سرور VPN متصل شوید",
        "Connected" to "متصل",
        "Connecting…" to "در حال اتصال…",
        "Connection" to "اتصال",
        "Connection details" to "جزئیات اتصال",
        "Continue" to "ادامه",
        "Contrast" to "کنتراست",
        "Copy logs" to "کپی گزارش‌ها",
        "Created by" to "ساخته‌شده توسط",
        "Curtain" to "پرده",
        "Custom" to "سفارشی",
        "Custom DoH (Not configured)" to "DoH سفارشی (پیکربندی نشده)",
        "Custom IP (Not configured)" to "IP سفارشی (پیکربندی نشده)",
        "Custom edge address" to "آدرس لبه سفارشی",
        "DNS" to "DNS",
        "DNS used inside the VPN tunnel for all app and browsing traffic." to "DNS داخل تونل VPN برای همه ترافیک برنامه و مرور.",
        "DNS used to resolve the VPN server address before the tunnel connects." to "DNS برای حل آدرس سرور VPN قبل از اتصال تونل.",
        "Dark" to "تاریک",
        "Default" to "پیش‌فرض",
        "Depth" to "عمق",
        "Disconnect" to "قطع اتصال",
        "Disconnected" to "قطع‌شده",
        "Display" to "نمایش",
        "Download" to "دانلود",
        "Dynamic color" to "رنگ پویا",
        "Elastic" to "کشسان",
        "Email" to "ایمیل",
        "Enable HTTP proxy" to "فعال‌سازی پروکسی HTTP",
        "English" to "English",
        "Enter the confirmation code sent to your email" to "کد تأییدی که به ایمیلتان ارسال شده را وارد کنید",
        "Excluded apps" to "برنامه‌های مستثنی",
        "Exit check" to "بررسی خروجی",
        "Expressive" to "بیانی",
        "Fade" to "محو",
        "Failed to load server list" to "بارگذاری فهرست سرورها ناموفق بود",
        "Fidelity" to "وفاداری",
        "Firefox account email" to "ایمیل حساب فایرفاکس",
        "Flip" to "چرخش",
        "Follow your wallpaper colors (Android 12+)" to "دنبال کردن رنگ‌های کاغذدیواری (اندروید ۱۲+)",
        "Font size" to "اندازه قلم",
        "FoxyVPN logs" to "گزارش‌های FoxyVPN",
        "Fruit Salad" to "سالاد میوه",
        "HTTP port" to "پورت HTTP",
        "Haptic feedback" to "بازخورد لمسی",
        "High" to "بالا",
        "Home" to "خانه",
        "Home screen design" to "طراحی صفحه اصلی",
        "Host or IP" to "میزبان یا IP",
        "Idle" to "بیکار",
        "Import" to "ورود",
        "Importing…" to "در حال ورود…",
        "Info" to "اطلاعات",
        "Interface" to "رابط کاربری",
        "Language" to "زبان",
        "Larger" to "بزرگ‌تر",
        "Largest" to "بزرگ‌ترین",
        "Leave empty to resolve the server hostname normally." to "خالی بگذارید تا نام میزبان سرور به‌طور عادی حل شود.",
        "Light" to "روشن",
        "List density" to "تراکم فهرست",
        "Live" to "زنده",
        "Local address" to "آدرس محلی",
        "Local port" to "پورت محلی",
        "Local proxy" to "پروکسی محلی",
        "Location" to "مکان",
        "Locations" to "مکان‌ها",
        "Logs" to "گزارش‌ها",
        "Manage accounts" to "مدیریت حساب‌ها",
        "Measure latency next to each location" to "اندازه‌گیری تأخیر کنار هر مکان",
        "Medium" to "متوسط",
        "Minimize animations across the app" to "کاهش انیمیشن‌ها در سراسر برنامه",
        "Monochrome" to "تک‌رنگ",
        "Monospace addresses" to "آدرس‌های monospace",
        "Multi-account list, quotas and batch import" to "فهرست چندحسابی، سهمیه‌ها و ورود دسته‌ای",
        "Navigation bar style" to "سبک نوار ناوبری",
        "Neutral" to "خنثی",
        "No accounts yet" to "هنوز حسابی نیست",
        "No accounts yet — add some from the Accounts screen." to "هنوز حسابی نیست — از صفحه حساب‌ها چند تا اضافه کنید.",
        "No log entries yet" to "هنوز ورودی گزارشی نیست",
        "No log entries yet." to "هنوز ورودی گزارشی نیست.",
        "No servers" to "سروری نیست",
        "No servers available" to "سروری در دسترس نیست",
        "No warnings or errors. Nothing has gone wrong." to "هیچ هشدار یا خطایی نیست. مشکلی پیش نیامده.",
        "None" to "هیچ‌کدام",
        "Normal" to "عادی",
        "Not connected" to "متصل نیست",
        "Not set" to "تنظیم نشده",
        "Nothing matches this filter." to "چیزی با این فیلتر مطابقت ندارد.",
        "Old" to "قدیمی",
        "One tap applies a curated look. Picking any color above returns to custom." to "با یک ضربه ظاهری انتخاب‌شده اعمال می‌شود. انتخاب هر رنگ بالا به حالت سفارشی برمی‌گردد.",
        "Password" to "رمز عبور",
        "Password (optional)" to "رمز عبور (اختیاری)",
        "Personalize" to "شخصی‌سازی",
        "Ping" to "پینگ",
        "Pixel" to "پیکسل",
        "Portal" to "پورتال",
        "Problems" to "مشکلات",
        "Proxy address" to "آدرس پروکسی",
        "Proxy credentials" to "اعتبارنامه پروکسی",
        "Proxy type" to "نوع پروکسی",
        "Proxy-only mode" to "حالت فقط-پروکسی",
        "Pure black background to save battery on OLED screens" to "پس‌زمینه مشکی خالص برای صرفه‌جویی باتری در صفحه‌های OLED",
        "Push" to "فشار",
        "Quota exhausted" to "سهمیه تمام شده",
        "Quota refresh failed" to "تازه‌سازی سهمیه ناموفق بود",
        "Rainbow" to "رنگین‌کمان",
        "Recent connection activity" to "فعالیت اخیر اتصال",
        "Recommended" to "پیشنهادی",
        "Reduce motion" to "کاهش حرکت",
        "Refresh ping" to "تازه‌سازی پینگ",
        "Refresh quotas" to "تازه‌سازی سهمیه‌ها",
        "Remove" to "حذف",
        "Remove account?" to "حذف حساب؟",
        "Reset all personalization" to "بازنشانی همه شخصی‌سازی‌ها",
        "Resolver DNS" to "DNS حل‌کننده",
        "Resolver IP address" to "آدرس IP حل‌کننده",
        "Save" to "ذخیره",
        "Screen transition" to "گذار صفحه",
        "Search apps" to "جستجوی برنامه‌ها",
        "Selected apps bypass the VPN and use your normal connection." to "برنامه‌های انتخاب‌شده از VPN عبور می‌کنند و از اتصال عادی شما استفاده می‌کنند.",
        "Settings" to "تنظیمات",
        "Show usage next to each location" to "نمایش مصرف کنار هر مکان",
        "Sign in" to "ورود",
        "Sign in with Firefox" to "ورود با فایرفاکس",
        "Sign out" to "خروج",
        "Sign out?" to "خروج؟",
        "Signature" to "امضا",
        "Slide" to "اسلاید",
        "Smaller" to "کوچک‌تر",
        "Smallest" to "کوچک‌ترین",
        "Split tunneling" to "تونل‌زنی تفکیکی",
        "Standard" to "استاندارد",
        "System" to "سیستم",
        "Tap the core to cancel" to "برای لغو روی هسته ضربه بزنید",
        "Tap the core to connect" to "برای اتصال روی هسته ضربه بزنید",
        "Theme" to "پوسته",
        "Theme profiles" to "پروفایل‌های پوسته",
        "This removes every saved account from this device. You can sign in again later." to "این کار همه حساب‌های ذخیره‌شده را از این دستگاه حذف می‌کند. بعداً می‌توانید دوباره وارد شوید.",
        "Tonal Spot" to "نقطه تونال",
        "Tunnel DNS" to "DNS تونل",
        "Upload" to "آپلود",
        "Username (optional)" to "نام کاربری (اختیاری)",
        "Verify" to "تأیید",
        "Vibrant" to "زنده",
        "Vibrate on connect control and theme changes" to "لرزش هنگام کنترل اتصال و تغییر پوسته",
    )

    fun t(key: String, lang: ResolvedLang): String {
        if (lang != ResolvedLang.Persian) return key
        return FA[key] ?: key
    }
}

enum class ResolvedLang { English, Persian }

val LocalL10n = compositionLocalOf { ResolvedLang.English }

@Composable
fun rememberResolvedLang(appLanguage: AppLanguage): ResolvedLang {
    val config = LocalConfiguration.current
    return remember(appLanguage, config.locales[0]?.language) {
        when (appLanguage) {
            AppLanguage.English -> ResolvedLang.English
            AppLanguage.Persian -> ResolvedLang.Persian
            AppLanguage.System -> {
                val tag = config.locales[0]?.language ?: "en"
                if (tag.startsWith("fa")) ResolvedLang.Persian else ResolvedLang.English
            }
        }
    }
}

/** Translate [key] using the ambient language. */
@Composable
fun s(key: String): String = L10n.t(key, LocalL10n.current)

