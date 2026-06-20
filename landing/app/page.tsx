import { SiteHeader } from "@/components/site-header"
import { HeroSection } from "@/components/hero-section"
import { HowItWorks } from "@/components/how-it-works"
import { FeaturesSection } from "@/components/features-section"
import { ReportSection } from "@/components/report-section"
import { PlansSection } from "@/components/plans-section"
import { SiteFooter } from "@/components/site-footer"

export default function Page() {
  return (
    <div className="min-h-screen bg-background">
      <SiteHeader />
      <main>
        <HeroSection />
        <HowItWorks />
        <FeaturesSection />
        <ReportSection />
        <PlansSection />
      </main>
      <SiteFooter />
    </div>
  )
}
